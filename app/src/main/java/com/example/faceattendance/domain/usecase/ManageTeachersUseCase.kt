package com.example.faceattendance.domain.usecase

import com.example.faceattendance.data.local.db.dao.UserDao
import com.example.faceattendance.data.local.db.entity.UserEntity
import com.example.faceattendance.data.local.db.entity.UserRole
import com.example.faceattendance.data.security.PasswordHasher
import com.example.faceattendance.domain.permission.PermissionChecker
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManageTeachersUseCase @Inject constructor(
    private val userDao: UserDao,
    private val passwordHasher: PasswordHasher,
    private val permissionChecker: PermissionChecker
) {
    fun getTeachers(role: UserRole): Flow<List<UserEntity>> {
        permissionChecker.requireAdmin(role)
        return userDao.getUsersByRole(UserRole.TEACHER)
    }

    suspend fun createTeacher(
        role: UserRole,
        username: String,
        displayName: String,
        email: String,
        initialPassword: String = "teacher123"
    ): Result<Long> = runCatching {
        permissionChecker.requireAdmin(role)

        val cleanUsername = username.trim()
        val cleanName = displayName.trim()
        val cleanEmail = email.trim()

        if (cleanUsername.isEmpty()) throw IllegalArgumentException("Tên đăng nhập không được để trống")
        if (cleanName.isEmpty()) throw IllegalArgumentException("Họ tên không được để trống")

        val existing = userDao.getByUsername(cleanUsername)
        if (existing != null) throw IllegalArgumentException("Tên đăng nhập '$cleanUsername' đã tồn tại trong hệ thống")

        val passwordHash = passwordHasher.hashPassword(initialPassword)

        userDao.insert(
            UserEntity(
                username = cleanUsername,
                passwordHash = passwordHash,
                role = UserRole.TEACHER,
                displayName = cleanName,
                email = cleanEmail,
                isActive = true
            )
        )
    }

    suspend fun toggleTeacherStatus(role: UserRole, teacherId: Long, isActive: Boolean): Result<Unit> = runCatching {
        permissionChecker.requireAdmin(role)
        userDao.setActiveStatus(teacherId, isActive)
    }

    suspend fun resetPassword(role: UserRole, teacherId: Long, newPassword: String = "teacher123"): Result<Unit> = runCatching {
        permissionChecker.requireAdmin(role)
        val hash = passwordHasher.hashPassword(newPassword)
        userDao.updatePassword(teacherId, hash)
    }

    suspend fun updateTeacher(
        role: UserRole,
        teacherId: Long,
        displayName: String,
        email: String
    ): Result<Unit> = runCatching {
        permissionChecker.requireAdmin(role)
        val user = userDao.getById(teacherId) ?: throw IllegalArgumentException("Không tìm thấy giáo viên")
        userDao.update(user.copy(displayName = displayName.trim(), email = email.trim()))
    }
}
