package com.example.faceattendance.domain.usecase

import com.example.faceattendance.data.local.db.dao.SessionWithDetails
import com.example.faceattendance.data.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTodaySessionsUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    operator fun invoke(teacherId: Long): Flow<List<SessionWithDetails>> {
        return sessionRepository.getTodaySessionsOfTeacher(teacherId)
    }
}
