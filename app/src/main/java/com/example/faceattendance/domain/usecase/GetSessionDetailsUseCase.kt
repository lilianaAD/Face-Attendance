package com.example.faceattendance.domain.usecase

import com.example.faceattendance.data.local.db.dao.AttendanceRecordItem
import com.example.faceattendance.data.local.db.dao.SessionWithDetails
import com.example.faceattendance.data.local.db.dao.StudentWithFaceInfo
import com.example.faceattendance.data.repository.SessionRepository
import com.example.faceattendance.data.repository.StudentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class FullSessionData(
    val sessionDetails: SessionWithDetails,
    val students: List<StudentWithFaceInfo>,
    val attendances: List<AttendanceRecordItem>
)

class GetSessionDetailsUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val studentRepository: StudentRepository
) {
    suspend fun getDetails(sessionId: Long): SessionWithDetails? {
        return sessionRepository.getSessionById(sessionId)
    }

    fun getStudentsWithFaceInfo(classId: Long): Flow<List<StudentWithFaceInfo>> {
        return studentRepository.getStudentsWithFaceInfoByClass(classId)
    }

    fun getSessionAttendances(sessionId: Long): Flow<List<AttendanceRecordItem>> {
        return studentRepository.getAttendancesForSession(sessionId)
    }

    suspend fun toggleSessionStatus(sessionId: Long, isOpen: Boolean): Result<Unit> {
        return if (isOpen) {
            sessionRepository.openSession(sessionId)
        } else {
            sessionRepository.closeSession(sessionId)
        }
    }
}
