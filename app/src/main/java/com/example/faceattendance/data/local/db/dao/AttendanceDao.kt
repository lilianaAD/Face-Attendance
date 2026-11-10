package com.example.faceattendance.data.local.db.dao

import androidx.room.*
import com.example.faceattendance.data.local.db.entity.AttendanceEntity
import com.example.faceattendance.data.local.db.entity.AttendanceMethod
import kotlinx.coroutines.flow.Flow

data class AttendanceRecordItem(
    @Embedded val attendance: AttendanceEntity,
    val mssv: String,
    val hoTen: String
)

@Dao
interface AttendanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attendance: AttendanceEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(attendance: AttendanceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(attendances: List<AttendanceEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoreAll(attendances: List<AttendanceEntity>): List<Long>

    @Update
    suspend fun update(attendance: AttendanceEntity)

    @Delete
    suspend fun delete(attendance: AttendanceEntity)

    @Query("DELETE FROM attendances WHERE sessionId = :sessionId AND studentId = :studentId")
    suspend fun removeAttendance(sessionId: Long, studentId: Long)

    @Query("SELECT * FROM attendances WHERE sessionId = :sessionId AND studentId = :studentId LIMIT 1")
    suspend fun getAttendance(sessionId: Long, studentId: Long): AttendanceEntity?

    @Query("""
        SELECT a.*, s.mssv, s.hoTen
        FROM attendances a
        INNER JOIN students s ON s.id = a.studentId
        WHERE a.sessionId = :sessionId
        ORDER BY s.hoTen ASC
    """)
    fun getAttendancesForSession(sessionId: Long): Flow<List<AttendanceRecordItem>>

    @Query("""
        SELECT * FROM attendances
        WHERE studentId = :studentId
        ORDER BY timestamp DESC
    """)
    fun getAttendancesForStudent(studentId: Long): Flow<List<AttendanceEntity>>

    @Query("SELECT COUNT(*) FROM attendances WHERE sessionId = :sessionId")
    suspend fun countBySession(sessionId: Long): Int

    @Query("SELECT COUNT(*) FROM attendances")
    suspend fun count(): Int

    @Query("SELECT * FROM attendances WHERE sessionId = :sessionId")
    suspend fun getBySession(sessionId: Long): List<AttendanceEntity>

    @Query("SELECT * FROM attendances WHERE sessionId IN (:sessionIds)")
    suspend fun getBySessionIds(sessionIds: List<Long>): List<AttendanceEntity>

    @Query("SELECT * FROM attendances ORDER BY timestamp DESC")
    suspend fun getAll(): List<AttendanceEntity>
}
