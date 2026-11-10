package com.example.faceattendance.domain.usecase

import com.example.faceattendance.data.local.db.entity.SessionEntity
import com.example.faceattendance.data.local.db.entity.UserRole
import com.example.faceattendance.data.repository.SessionRepository
import com.example.faceattendance.domain.permission.PermissionChecker
import javax.inject.Inject

class CreateSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val permissionChecker: PermissionChecker
) {
    suspend operator fun invoke(
        userId: Long,
        role: UserRole,
        courseId: Long,
        classId: Long,
        sessionDate: String,
        startTime: String,
        endTime: String,
        room: String
    ): Result<SessionEntity> = runCatching {
        // 1. Phân quyền: Giáo viên chỉ được tạo buổi học cho lớp mình phụ trách
        permissionChecker.verifyClassAccess(userId, role, classId)

        // 2. Validate giờ bắt đầu và kết thúc
        if (startTime >= endTime) {
            throw IllegalArgumentException("Thời gian bắt đầu ($startTime) phải trước thời gian kết thúc ($endTime)")
        }

        if (room.trim().isEmpty()) {
            throw IllegalArgumentException("Phòng học không được để trống")
        }

        // 3. Tạo buổi học kèm kiểm tra trùng lịch trong SessionRepository
        sessionRepository.createSession(
            courseId = courseId,
            classId = classId,
            sessionDate = sessionDate,
            startTime = startTime,
            endTime = endTime,
            room = room.trim()
        ).getOrThrow()
    }
}
