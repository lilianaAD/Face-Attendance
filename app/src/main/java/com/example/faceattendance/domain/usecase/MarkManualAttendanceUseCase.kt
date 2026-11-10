package com.example.faceattendance.domain.usecase

import com.example.faceattendance.data.repository.AttendanceRepository
import com.example.faceattendance.data.repository.StudentAttendanceRow
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MarkManualAttendanceUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository
) {
    suspend fun toggleAttendance(
        sessionId: Long,
        studentId: Long,
        currentlyPresent: Boolean,
        note: String? = "Điểm danh thủ công"
    ): Boolean {
        return if (currentlyPresent) {
            attendanceRepository.removeAttendance(sessionId, studentId)
        } else {
            attendanceRepository.markAttendance(
                sessionId = sessionId,
                studentId = studentId,
                confidence = 1.0f,
                method = "MANUAL",
                note = note
            )
        }
    }

    suspend fun getRoster(sessionId: Long, classId: Long): List<StudentAttendanceRow> {
        return attendanceRepository.getStudentsWithAttendance(sessionId, classId)
    }

    fun observeRoster(sessionId: Long, classId: Long): Flow<List<StudentAttendanceRow>> {
        return attendanceRepository.observeStudentsWithAttendance(sessionId, classId)
    }
}
