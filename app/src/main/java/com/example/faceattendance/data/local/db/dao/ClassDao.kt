package com.example.faceattendance.data.local.db.dao

import androidx.room.*
import com.example.faceattendance.data.local.db.entity.ClassEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(classEntity: ClassEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(classes: List<ClassEntity>): List<Long>

    @Update
    suspend fun update(classEntity: ClassEntity)

    @Delete
    suspend fun delete(classEntity: ClassEntity)

    @Query("SELECT * FROM classes WHERE id = :id")
    suspend fun getById(id: Long): ClassEntity?

    @Query("SELECT * FROM classes WHERE classCode = :code LIMIT 1")
    suspend fun getByCode(code: String): ClassEntity?

    @Query("SELECT * FROM classes ORDER BY classCode ASC")
    fun getAllClasses(): Flow<List<ClassEntity>>

    @Query("SELECT * FROM classes ORDER BY classCode ASC")
    suspend fun getAllClassesSync(): List<ClassEntity>

    @Query("""
        SELECT c.* FROM classes c
        INNER JOIN teacher_classes tc ON tc.classId = c.id
        WHERE tc.teacherId = :teacherId
        ORDER BY c.classCode ASC
    """)
    fun getClassesForTeacher(teacherId: Long): Flow<List<ClassEntity>>

    @Query("SELECT COUNT(*) FROM classes")
    suspend fun count(): Int
}
