package com.example.faceattendance.data.local.db.dao

import androidx.room.*
import com.example.faceattendance.data.local.db.entity.TeacherClassEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeacherClassDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(assignment: TeacherClassEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(assignments: List<TeacherClassEntity>)

    @Delete
    suspend fun delete(assignment: TeacherClassEntity)

    @Query("DELETE FROM teacher_classes WHERE teacherId = :teacherId AND classId = :classId")
    suspend fun removeAssignment(teacherId: Long, classId: Long)

    @Query("SELECT classId FROM teacher_classes WHERE teacherId = :teacherId")
    fun getClassIdsForTeacher(teacherId: Long): Flow<List<Long>>

    @Query("SELECT teacherId FROM teacher_classes WHERE classId = :classId")
    fun getTeacherIdsForClass(classId: Long): Flow<List<Long>>

    @Query("SELECT COUNT(*) FROM teacher_classes WHERE teacherId = :teacherId AND classId = :classId")
    suspend fun isTeacherAssignedToClass(teacherId: Long, classId: Long): Int

    @Query("SELECT * FROM teacher_classes")
    suspend fun getAllAssignmentsSync(): List<TeacherClassEntity>
}
