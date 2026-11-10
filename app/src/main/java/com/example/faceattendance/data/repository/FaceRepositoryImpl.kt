package com.example.faceattendance.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.faceattendance.data.local.db.dao.FaceEmbeddingDao
import com.example.faceattendance.data.local.db.dao.StudentEmbeddingRecord
import com.example.faceattendance.data.local.db.entity.FaceEmbeddingEntity
import com.example.faceattendance.data.ml.DuplicateMatch
import com.example.faceattendance.data.ml.FaceMatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FaceRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val faceEmbeddingDao: FaceEmbeddingDao
) : FaceRepository {

    override fun getEmbeddingsForStudent(studentId: Long): Flow<List<FaceEmbeddingEntity>> {
        return faceEmbeddingDao.getEmbeddingsForStudent(studentId)
    }

    override suspend fun getAllEmbeddings(): List<StudentEmbeddingRecord> = withContext(Dispatchers.IO) {
        faceEmbeddingDao.getAllEmbeddings()
    }

    override suspend fun getEmbeddingsByClass(classId: Long): List<StudentEmbeddingRecord> = withContext(Dispatchers.IO) {
        faceEmbeddingDao.getEmbeddingsByClass(classId)
    }

    override suspend fun saveFaceEmbedding(
        studentId: Long,
        bitmap: Bitmap,
        embedding: FloatArray,
        qualityScore: Float,
        pose: String
    ): Result<FaceEmbeddingEntity> = withContext(Dispatchers.IO) {
        try {
            // 1. Lưu file ảnh vào bộ nhớ trong
            val facesDir = File(context.filesDir, "faces").apply {
                if (!exists()) mkdirs()
            }
            val fileName = "student_${studentId}_${pose.lowercase()}_${System.currentTimeMillis()}.jpg"
            val file = File(facesDir, fileName)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }

            // 2. Tạo bản ghi Room Entity
            val entity = FaceEmbeddingEntity(
                studentId = studentId,
                embedding = embedding,
                imagePath = file.absolutePath,
                qualityScore = qualityScore,
                pose = pose.uppercase(),
                createdAt = System.currentTimeMillis()
            )

            val newId = faceEmbeddingDao.insert(entity)
            Result.success(entity.copy(id = newId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkDuplicate(
        studentId: Long,
        newEmbedding: FloatArray,
        threshold: Float
    ): DuplicateMatch? = withContext(Dispatchers.IO) {
        val allExisting = faceEmbeddingDao.getAllEmbeddings()
        FaceMatcher.checkDuplicate(studentId, newEmbedding, allExisting, threshold)
    }

    override suspend fun deleteEmbedding(embedding: FaceEmbeddingEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Xóa file ảnh khỏi bộ nhớ máy
            val file = File(embedding.imagePath)
            if (file.exists()) {
                file.delete()
            }
            faceEmbeddingDao.delete(embedding)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAllEmbeddingsForStudent(studentId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            faceEmbeddingDao.deleteByStudentId(studentId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
