package com.example.faceattendance.data.local.db.dao

import androidx.room.*
import com.example.faceattendance.data.local.db.entity.CourseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(course: CourseEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(courses: List<CourseEntity>): List<Long>

    @Update
    suspend fun update(course: CourseEntity)

    @Delete
    suspend fun delete(course: CourseEntity)

    @Query("SELECT * FROM courses WHERE id = :id")
    suspend fun getById(id: Long): CourseEntity?

    @Query("SELECT * FROM courses WHERE courseCode = :code LIMIT 1")
    suspend fun getByCode(code: String): CourseEntity?

    @Query("SELECT * FROM courses ORDER BY courseCode ASC")
    fun getAllCourses(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses ORDER BY courseCode ASC")
    suspend fun getAllCoursesSync(): List<CourseEntity>

    @Query("SELECT COUNT(*) FROM courses")
    suspend fun count(): Int
}
