package com.example.faceattendance.data.local.db.dao

import androidx.room.*
import com.example.faceattendance.data.local.db.entity.SessionEntity
import com.example.faceattendance.data.local.db.entity.SessionStatus
import kotlinx.coroutines.flow.Flow

data class SessionWithDetails(
    @Embedded val session: SessionEntity,
    val courseName: String,
    val courseCode: String,
    val className: String,
    val classCode: String,
    val totalStudents: Int,
    val attendedCount: Int
)

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(session: SessionEntity): Long

    @Update
    suspend fun update(session: SessionEntity)

    @Delete
    suspend fun delete(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getById(id: Long): SessionEntity?

    @Query("""
        SELECT * FROM sessions 
        WHERE courseId = :courseId 
          AND classId = :classId 
          AND sessionDate = :sessionDate 
          AND startTime = :startTime 
        LIMIT 1
    """)
    suspend fun findCollision(courseId: Long, classId: Long, sessionDate: String, startTime: String): SessionEntity?

    @Query("""
        SELECT s.*, 
               c.courseName, c.courseCode, 
               cl.className, cl.classCode,
               (SELECT COUNT(*) FROM students st WHERE st.classId = s.classId) AS totalStudents,
               (SELECT COUNT(DISTINCT a.studentId) FROM attendances a WHERE a.sessionId = s.id) AS attendedCount
        FROM sessions s
        INNER JOIN courses c ON c.id = s.courseId
        INNER JOIN classes cl ON cl.id = s.classId
        WHERE s.id = :sessionId
    """)
    fun getSessionDetails(sessionId: Long): Flow<SessionWithDetails?>

    @Query("""
        SELECT s.*, 
               c.courseName, c.courseCode, 
               cl.className, cl.classCode,
               (SELECT COUNT(*) FROM students st WHERE st.classId = s.classId) AS totalStudents,
               (SELECT COUNT(DISTINCT a.studentId) FROM attendances a WHERE a.sessionId = s.id) AS attendedCount
        FROM sessions s
        INNER JOIN courses c ON c.id = s.courseId
        INNER JOIN classes cl ON cl.id = s.classId
        INNER JOIN teacher_classes tc ON tc.classId = s.classId
        WHERE tc.teacherId = :teacherId AND s.sessionDate = :today
        ORDER BY s.startTime ASC
    """)
    fun getTodaySessionsForTeacher(teacherId: Long, today: String): Flow<List<SessionWithDetails>>

    @Query("""
        SELECT s.*, 
               c.courseName, c.courseCode, 
               cl.className, cl.classCode,
               (SELECT COUNT(*) FROM students st WHERE st.classId = s.classId) AS totalStudents,
               (SELECT COUNT(DISTINCT a.studentId) FROM attendances a WHERE a.sessionId = s.id) AS attendedCount
        FROM sessions s
        INNER JOIN courses c ON c.id = s.courseId
        INNER JOIN classes cl ON cl.id = s.classId
        WHERE s.sessionDate = :today
        ORDER BY s.startTime ASC
    """)
    fun getAllTodaySessions(today: String): Flow<List<SessionWithDetails>>

    @Query("UPDATE sessions SET status = :status WHERE id = :sessionId")
    suspend fun updateStatus(sessionId: Long, status: SessionStatus)

    @Query("SELECT COUNT(*) FROM sessions")
    suspend fun count(): Int

    @Query("SELECT * FROM sessions WHERE classId = :classId AND sessionDate LIKE :month || '%' ORDER BY sessionDate ASC, startTime ASC")
    suspend fun getByClassAndMonth(classId: Long, month: String): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE classId = :classId ORDER BY sessionDate DESC, startTime DESC")
    suspend fun getByClassId(classId: Long): List<SessionEntity>

    @Query("SELECT * FROM sessions ORDER BY sessionDate DESC, startTime DESC")
    suspend fun getAllSessions(): List<SessionEntity>
}
