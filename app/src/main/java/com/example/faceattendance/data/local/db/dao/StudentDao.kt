package com.example.faceattendance.data.local.db.dao

import androidx.room.*
import com.example.faceattendance.data.local.db.entity.StudentEntity
import kotlinx.coroutines.flow.Flow

data class StudentWithFaceInfo(
    @Embedded val student: StudentEntity,
    val faceCount: Int
)

@Dao
interface StudentDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(student: StudentEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(students: List<StudentEntity>): List<Long>

    @Update
    suspend fun update(student: StudentEntity)

    @Delete
    suspend fun delete(student: StudentEntity)

    @Query("SELECT * FROM students WHERE id = :id")
    suspend fun getById(id: Long): StudentEntity?

    @Query("SELECT * FROM students WHERE mssv = :mssv LIMIT 1")
    suspend fun getByMssv(mssv: String): StudentEntity?

    @Query("SELECT * FROM students WHERE classId = :classId ORDER BY hoTen ASC")
    fun getStudentsByClass(classId: Long): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE classId = :classId ORDER BY hoTen ASC")
    suspend fun getStudentsByClassSync(classId: Long): List<StudentEntity>

    @Query("SELECT * FROM students WHERE classId = :classId ORDER BY hoTen ASC")
    suspend fun getByClassId(classId: Long): List<StudentEntity>

    @Query("SELECT * FROM students ORDER BY hoTen ASC")
    suspend fun getAllStudentsSync(): List<StudentEntity>

    @Query("""
        SELECT s.*, 
               (SELECT COUNT(*) FROM face_embeddings fe WHERE fe.studentId = s.id) AS faceCount
        FROM students s
        WHERE s.classId = :classId
        ORDER BY s.hoTen ASC
    """)
    fun getStudentsWithFaceInfoByClass(classId: Long): Flow<List<StudentWithFaceInfo>>

    @Query("""
        SELECT s.*, 
               (SELECT COUNT(*) FROM face_embeddings fe WHERE fe.studentId = s.id) AS faceCount
        FROM students s
        ORDER BY s.hoTen ASC
    """)
    fun getAllStudentsWithFaceInfo(): Flow<List<StudentWithFaceInfo>>

    @Query("SELECT COUNT(*) FROM students")
    suspend fun count(): Int

    @Query("""
        SELECT COUNT(DISTINCT s.id) FROM students s
        LEFT JOIN face_embeddings fe ON fe.studentId = s.id
        WHERE s.classId = :classId AND fe.id IS NULL
    """)
    suspend fun countStudentsWithoutFaceByClass(classId: Long): Int
}
