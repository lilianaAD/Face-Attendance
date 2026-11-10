package com.example.faceattendance.domain.usecase

import com.example.faceattendance.data.local.db.entity.UserEntity
import com.example.faceattendance.data.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(username: String, password: String): Result<UserEntity> {
        val trimmedUser = username.trim()
        if (trimmedUser.isEmpty()) {
            return Result.failure(IllegalArgumentException("Vui lòng nhập tên đăng nhập"))
        }
        if (password.isEmpty()) {
            return Result.failure(IllegalArgumentException("Vui lòng nhập mật khẩu"))
        }
        return authRepository.login(trimmedUser, password)
    }
}
