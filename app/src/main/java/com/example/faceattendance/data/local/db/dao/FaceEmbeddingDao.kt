package com.example.faceattendance.data.local.db.dao

import androidx.room.*
import com.example.faceattendance.data.local.db.entity.FaceEmbeddingEntity
import kotlinx.coroutines.flow.Flow

data class StudentEmbeddingRecord(
    val embeddingId: Long,
    val studentId: Long,
    val mssv: String,
    val hoTen: String,
    val embedding: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as StudentEmbeddingRecord
        return embeddingId == other.embeddingId && studentId == other.studentId && embedding.contentEquals(other.embedding)
    }

    override fun hashCode(): Int {
        var result = embeddingId.hashCode()
        result = 31 * result + studentId.hashCode()
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}

@Dao
interface FaceEmbeddingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(embedding: FaceEmbeddingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(embeddings: List<FaceEmbeddingEntity>): List<Long>

    @Delete
    suspend fun delete(embedding: FaceEmbeddingEntity)

    @Query("DELETE FROM face_embeddings WHERE studentId = :studentId")
    suspend fun deleteByStudentId(studentId: Long)

    @Query("SELECT * FROM face_embeddings WHERE studentId = :studentId ORDER BY createdAt DESC")
    fun getEmbeddingsForStudent(studentId: Long): Flow<List<FaceEmbeddingEntity>>

    @Query("""
        SELECT fe.id AS embeddingId, fe.studentId, s.mssv, s.hoTen, fe.embedding
        FROM face_embeddings fe
        INNER JOIN students s ON s.id = fe.studentId
        WHERE s.classId = :classId
    """)
    suspend fun getEmbeddingsByClass(classId: Long): List<StudentEmbeddingRecord>

    @Query("""
        SELECT fe.id AS embeddingId, fe.studentId, s.mssv, s.hoTen, fe.embedding
        FROM face_embeddings fe
        INNER JOIN students s ON s.id = fe.studentId
    """)
    suspend fun getAllEmbeddings(): List<StudentEmbeddingRecord>

    @Query("SELECT COUNT(*) FROM face_embeddings")
    suspend fun count(): Int

    @Query("SELECT * FROM face_embeddings")
    suspend fun getAllFaceEmbeddingsSync(): List<FaceEmbeddingEntity>
}
