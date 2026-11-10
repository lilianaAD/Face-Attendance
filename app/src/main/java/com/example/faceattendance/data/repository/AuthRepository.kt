package com.example.faceattendance.data.repository

import com.example.faceattendance.data.local.db.dao.UserDao
import com.example.faceattendance.data.local.db.entity.UserEntity
import com.example.faceattendance.data.local.db.entity.UserRole
import com.example.faceattendance.data.local.preferences.AuthDataStore
import com.example.faceattendance.data.local.preferences.AuthSession
import com.example.faceattendance.data.security.PasswordHasher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface AuthRepository {
    val authSession: Flow<AuthSession>
    suspend fun login(username: String, password: String): Result<UserEntity>
    suspend fun logout()
    suspend fun changePassword(userId: Long, oldPass: String, newPass: String): Result<Unit>
    suspend fun getCurrentUser(): UserEntity?
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val authDataStore: AuthDataStore,
    private val passwordHasher: PasswordHasher
) : AuthRepository {

    override val authSession: Flow<AuthSession> = authDataStore.authSessionFlow

    override suspend fun login(username: String, password: String): Result<UserEntity> {
        val user = userDao.getByUsername(username.trim())
            ?: return Result.failure(Exception("Tên đăng nhập hoặc mật khẩu không chính xác"))

        if (!user.isActive) {
            return Result.failure(Exception("Tài khoản này đã bị tạm khóa. Vui lòng liên hệ Quản trị viên."))
        }

        val isValid = passwordHasher.verifyPassword(password, user.passwordHash)
        if (!isValid) {
            return Result.failure(Exception("Tên đăng nhập hoặc mật khẩu không chính xác"))
        }

        authDataStore.saveSession(
            userId = user.id,
            username = user.username,
            displayName = user.displayName,
            role = user.role
        )

        return Result.success(user)
    }

    override suspend fun logout() {
        authDataStore.clearSession()
    }

    override suspend fun changePassword(userId: Long, oldPass: String, newPass: String): Result<Unit> {
        val user = userDao.getById(userId)
            ?: return Result.failure(Exception("Không tìm thấy người dùng"))

        if (!passwordHasher.verifyPassword(oldPass, user.passwordHash)) {
            return Result.failure(Exception("Mật khẩu hiện tại không đúng"))
        }

        if (newPass.length < 6) {
            return Result.failure(Exception("Mật khẩu mới phải có ít nhất 6 ký tự"))
        }

        val newHash = passwordHasher.hashPassword(newPass)
        userDao.updatePassword(userId, newHash)
        return Result.success(Unit)
    }

    override suspend fun getCurrentUser(): UserEntity? {
        var currentUserId: Long? = null
        authDataStore.authSessionFlow.collect { session ->
            currentUserId = if (session.isLoggedIn) session.userId else null
            return@collect
        }
        return currentUserId?.let { userDao.getById(it) }
    }
}
