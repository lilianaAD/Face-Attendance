package com.example.faceattendance.domain.usecase

import com.example.faceattendance.data.local.db.entity.FaceEmbeddingEntity
import com.example.faceattendance.data.repository.FaceRepository
import javax.inject.Inject

class DeleteFaceEmbeddingUseCase @Inject constructor(
    private val faceRepository: FaceRepository
) {
    suspend fun deleteEmbedding(embedding: FaceEmbeddingEntity): Result<Unit> {
        return faceRepository.deleteEmbedding(embedding)
    }

    suspend fun deleteAllEmbeddingsForStudent(studentId: Long): Result<Unit> {
        return faceRepository.deleteAllEmbeddingsForStudent(studentId)
    }
}
