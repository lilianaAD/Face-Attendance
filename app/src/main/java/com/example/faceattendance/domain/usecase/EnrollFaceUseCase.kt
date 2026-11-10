package com.example.faceattendance.domain.usecase

import android.graphics.Bitmap
import com.example.faceattendance.data.local.db.entity.FaceEmbeddingEntity
import com.example.faceattendance.data.ml.DuplicateMatch
import com.example.faceattendance.data.repository.FaceRepository
import javax.inject.Inject

sealed class EnrollResult {
    data class Success(val entity: FaceEmbeddingEntity) : EnrollResult()
    data class DuplicateWarning(val match: DuplicateMatch) : EnrollResult()
    data class Failure(val message: String) : EnrollResult()
}

class EnrollFaceUseCase @Inject constructor(
    private val faceRepository: FaceRepository
) {
    suspend operator fun invoke(
        studentId: Long,
        bitmap: Bitmap,
        embedding: FloatArray,
        qualityScore: Float,
        pose: String,
        forceOverrideDuplicate: Boolean = false
    ): EnrollResult {
        // 1. Kiểm tra trùng lặp với sinh viên khác trong cơ sở dữ liệu
        if (!forceOverrideDuplicate) {
            val duplicate = faceRepository.checkDuplicate(studentId, newEmbedding = embedding, threshold = 0.75f)
            if (duplicate != null) {
                return EnrollResult.DuplicateWarning(duplicate)
            }
        }

        // 2. Lưu mẫu ảnh và embedding vào Repository
        val saveResult = faceRepository.saveFaceEmbedding(
            studentId = studentId,
            bitmap = bitmap,
            embedding = embedding,
            qualityScore = qualityScore,
            pose = pose
        )

        return saveResult.fold(
            onSuccess = { EnrollResult.Success(it) },
            onFailure = { EnrollResult.Failure(it.message ?: "Lỗi lưu mẫu khuôn mặt") }
        )
    }
}
