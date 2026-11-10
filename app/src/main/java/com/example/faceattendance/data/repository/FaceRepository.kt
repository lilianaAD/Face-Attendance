package com.example.faceattendance.data.repository

import android.graphics.Bitmap
import com.example.faceattendance.data.local.db.dao.FaceEmbeddingDao
import com.example.faceattendance.data.local.db.dao.StudentEmbeddingRecord
import com.example.faceattendance.data.local.db.entity.FaceEmbeddingEntity
import com.example.faceattendance.data.ml.DuplicateMatch
import kotlinx.coroutines.flow.Flow

interface FaceRepository {
    fun getEmbeddingsForStudent(studentId: Long): Flow<List<FaceEmbeddingEntity>>

    suspend fun getAllEmbeddings(): List<StudentEmbeddingRecord>

    suspend fun getEmbeddingsByClass(classId: Long): List<StudentEmbeddingRecord>

    suspend fun saveFaceEmbedding(
        studentId: Long,
        bitmap: Bitmap,
        embedding: FloatArray,
        qualityScore: Float,
        pose: String
    ): Result<FaceEmbeddingEntity>

    suspend fun checkDuplicate(
        studentId: Long,
        newEmbedding: FloatArray,
        threshold: Float = 0.75f
    ): DuplicateMatch?

    suspend fun deleteEmbedding(embedding: FaceEmbeddingEntity): Result<Unit>

    suspend fun deleteAllEmbeddingsForStudent(studentId: Long): Result<Unit>
}
