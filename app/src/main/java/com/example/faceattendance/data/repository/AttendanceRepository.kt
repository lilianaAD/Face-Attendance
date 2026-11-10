package com.example.faceattendance.data.repository

import com.example.faceattendance.data.local.db.entity.AttendanceEntity
import com.example.faceattendance.data.local.db.entity.StudentEntity
import kotlinx.coroutines.flow.Flow

enum class AttendanceStatus {
    PRESENT,
    UNCERTAIN,
    ABSENT
}

data class AttendanceRecord(
    val studentId: Long,
    val confidence: Float,
    val method: String, // "FACE", "FACE_LOW_CONF", "MANUAL"
    val imagePath: String = "",
    val note: String? = null
)

data class StudentAttendanceRow(
    val student: StudentEntity,
    val attendance: AttendanceEntity?,
    val status: AttendanceStatus
)

interface AttendanceRepository {
    /**
     * Ghi điểm danh cho 1 SV (INSERT OR IGNORE)
     * @return true nếu insert mới, false nếu đã tồn tại hoặc lỗi
     */
    suspend fun markAttendance(
        sessionId: Long,
        studentId: Long,
        confidence: Float,
        method: String,
        imagePath: String = "",
        note: String? = null
    ): Boolean

    /**
     * Batch insert nhiều bản ghi (dùng transaction)
     * @return số bản ghi thêm mới
     */
    suspend fun markBatchAttendance(
        sessionId: Long,
        records: List<AttendanceRecord>
    ): Int

    /**
     * Xóa 1 bản ghi điểm danh
     */
    suspend fun removeAttendance(sessionId: Long, studentId: Long): Boolean

    /**
     * Lấy tất cả SV trong lớp kèm trạng thái điểm danh của session
     */
    suspend fun getStudentsWithAttendance(
        sessionId: Long,
        classId: Long
    ): List<StudentAttendanceRow>

    /**
     * Lấy dòng trạng thái điểm danh thời gian thực (Flow)
     */
    fun observeStudentsWithAttendance(
        sessionId: Long,
        classId: Long
    ): Flow<List<StudentAttendanceRow>>

    /**
     * Đếm tổng số sinh viên đã điểm danh trong session
     */
    suspend fun getAttendedCount(sessionId: Long): Int
}
