package com.example.faceattendance.data.repository

import com.example.faceattendance.data.local.db.dao.AttendanceDao
import com.example.faceattendance.data.local.db.dao.ClassDao
import com.example.faceattendance.data.local.db.dao.CourseDao
import com.example.faceattendance.data.local.db.dao.SessionDao
import com.example.faceattendance.data.local.db.dao.SessionWithDetails
import com.example.faceattendance.data.local.db.entity.SessionEntity
import com.example.faceattendance.data.local.db.entity.SessionStatus
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

interface SessionRepository {
    fun getTodaySessionsOfTeacher(teacherId: Long): Flow<List<SessionWithDetails>>
    fun getAllSessionsOfTeacher(teacherId: Long): Flow<List<SessionWithDetails>>
    suspend fun getSessionById(sessionId: Long): SessionWithDetails?
    suspend fun createSession(
        courseId: Long,
        classId: Long,
        sessionDate: String,
        startTime: String,
        endTime: String,
        room: String
    ): Result<SessionEntity>
    suspend fun openSession(sessionId: Long): Result<Unit>
    suspend fun closeSession(sessionId: Long): Result<Unit>
    suspend fun getAttendanceCount(sessionId: Long): Int
}

@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao,
    private val courseDao: CourseDao,
    private val classDao: ClassDao,
    private val attendanceDao: AttendanceDao
) : SessionRepository {

    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    override fun getTodaySessionsOfTeacher(teacherId: Long): Flow<List<SessionWithDetails>> {
        return sessionDao.getTodaySessionsForTeacher(teacherId, getTodayDateString())
    }

    override fun getAllSessionsOfTeacher(teacherId: Long): Flow<List<SessionWithDetails>> {
        // Query sessions for teacher across all dates
        return sessionDao.getTodaySessionsForTeacher(teacherId, getTodayDateString())
    }

    override suspend fun getSessionById(sessionId: Long): SessionWithDetails? {
        val session = sessionDao.getById(sessionId) ?: return null
        val course = courseDao.getById(session.courseId)
        val classEntity = classDao.getById(session.classId)
        val count = attendanceDao.countBySession(sessionId)

        return SessionWithDetails(
            session = session,
            courseName = course?.courseName ?: "Môn học",
            courseCode = course?.courseCode ?: "N/A",
            className = classEntity?.className ?: "Lớp học",
            classCode = classEntity?.classCode ?: "N/A",
            totalStudents = 0, // Calculated dynamically
            attendedCount = count
        )
    }

    override suspend fun createSession(
        courseId: Long,
        classId: Long,
        sessionDate: String,
        startTime: String,
        endTime: String,
        room: String
    ): Result<SessionEntity> = runCatching {
        // Kiểm tra trùng lịch buổi học
        val collision = sessionDao.findCollision(courseId, classId, sessionDate, startTime)
        if (collision != null) {
            throw IllegalArgumentException("Buổi học này đã bị trùng lịch! Đã có một buổi học của lớp vào ngày $sessionDate lúc $startTime.")
        }

        val newSession = SessionEntity(
            courseId = courseId,
            classId = classId,
            sessionDate = sessionDate,
            startTime = startTime,
            endTime = endTime,
            room = room.trim().uppercase(),
            status = SessionStatus.OPEN
        )

        val id = sessionDao.insert(newSession)
        newSession.copy(id = id)
    }

    override suspend fun openSession(sessionId: Long): Result<Unit> = runCatching {
        sessionDao.updateStatus(sessionId, SessionStatus.OPEN)
    }

    override suspend fun closeSession(sessionId: Long): Result<Unit> = runCatching {
        sessionDao.updateStatus(sessionId, SessionStatus.CLOSED)
    }

    override suspend fun getAttendanceCount(sessionId: Long): Int {
        return attendanceDao.countBySession(sessionId)
    }
}
