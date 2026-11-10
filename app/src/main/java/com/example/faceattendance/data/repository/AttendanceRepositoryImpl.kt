package com.example.faceattendance.data.repository

import androidx.room.withTransaction
import com.example.faceattendance.data.local.db.AppDatabase
import com.example.faceattendance.data.local.db.dao.AttendanceDao
import com.example.faceattendance.data.local.db.dao.FaceEmbeddingDao
import com.example.faceattendance.data.local.db.dao.StudentDao
import com.example.faceattendance.data.local.db.entity.AttendanceEntity
import com.example.faceattendance.data.local.db.entity.AttendanceMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val attendanceDao: AttendanceDao,
    private val faceEmbeddingDao: FaceEmbeddingDao,
    private val studentDao: StudentDao
) : AttendanceRepository {

    override suspend fun markAttendance(
        sessionId: Long,
        studentId: Long,
        confidence: Float,
        method: String,
        imagePath: String,
        note: String?
    ): Boolean = withContext(Dispatchers.IO) {
        val attMethod = when (method.uppercase()) {
            "FACE" -> AttendanceMethod.FACE
            "FACE_LOW_CONF" -> AttendanceMethod.FACE_LOW_CONF
            else -> AttendanceMethod.MANUAL
        }

        val entity = AttendanceEntity(
            sessionId = sessionId,
            studentId = studentId,
            timestamp = System.currentTimeMillis(),
            confidence = confidence,
            method = attMethod,
            imagePath = imagePath.ifEmpty { null },
            note = note
        )

        val id = attendanceDao.insertIgnore(entity)
        id > 0
    }

    override suspend fun markBatchAttendance(
        sessionId: Long,
        records: List<AttendanceRecord>
    ): Int = withContext(Dispatchers.IO) {
        if (records.isEmpty()) return@withContext 0

        database.withTransaction {
            val entities = records.map { r ->
                val attMethod = when (r.method.uppercase()) {
                    "FACE" -> AttendanceMethod.FACE
                    "FACE_LOW_CONF" -> AttendanceMethod.FACE_LOW_CONF
                    else -> AttendanceMethod.MANUAL
                }
                AttendanceEntity(
                    sessionId = sessionId,
                    studentId = r.studentId,
                    timestamp = System.currentTimeMillis(),
                    confidence = r.confidence,
                    method = attMethod,
                    imagePath = r.imagePath.ifEmpty { null },
                    note = r.note
                )
            }

            val insertedIds = attendanceDao.insertIgnoreAll(entities)
            insertedIds.count { it > 0 }
        }
    }

    override suspend fun removeAttendance(sessionId: Long, studentId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            attendanceDao.removeAttendance(sessionId, studentId)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getStudentsWithAttendance(
        sessionId: Long,
        classId: Long
    ): List<StudentAttendanceRow> = withContext(Dispatchers.IO) {
        val students = studentDao.getStudentsByClassSync(classId)

        students.map { student ->
            val att = attendanceDao.getAttendance(sessionId, student.id)
            val status = when {
                att == null -> AttendanceStatus.ABSENT
                att.method == AttendanceMethod.FACE_LOW_CONF -> AttendanceStatus.UNCERTAIN
                else -> AttendanceStatus.PRESENT
            }
            StudentAttendanceRow(
                student = student,
                attendance = att,
                status = status
            )
        }
    }

    override fun observeStudentsWithAttendance(
        sessionId: Long,
        classId: Long
    ): Flow<List<StudentAttendanceRow>> {
        val studentsFlow = studentDao.getStudentsByClass(classId)
        val attendancesFlow = attendanceDao.getAttendancesForSession(sessionId)

        return combine(studentsFlow, attendancesFlow) { students, attendances ->
            val attMap = attendances.associateBy { it.attendance.studentId }
            students.map { student ->
                val record = attMap[student.id]
                val att = record?.attendance
                val status = when {
                    att == null -> AttendanceStatus.ABSENT
                    att.method == AttendanceMethod.FACE_LOW_CONF -> AttendanceStatus.UNCERTAIN
                    else -> AttendanceStatus.PRESENT
                }
                StudentAttendanceRow(
                    student = student,
                    attendance = att,
                    status = status
                )
            }
        }
    }

    override suspend fun getAttendedCount(sessionId: Long): Int = withContext(Dispatchers.IO) {
        attendanceDao.countBySession(sessionId)
    }
}
