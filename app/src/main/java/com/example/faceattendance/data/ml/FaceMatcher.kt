package com.example.faceattendance.data.ml

import com.example.faceattendance.data.local.db.dao.StudentEmbeddingRecord
import kotlin.math.sqrt

data class DuplicateMatch(
    val studentId: Long,
    val mssv: String,
    val hoTen: String,
    val similarity: Float
)

enum class MatchDecision {
    AUTO_ACCEPT,  // similarity >= 0.65
    NEED_CONFIRM, // 0.55 <= similarity < 0.65
    REJECT        // similarity < 0.55
}

data class FaceMatchResult(
    val studentId: Long,
    val mssv: String,
    val hoTen: String,
    val similarity: Float,
    val decision: MatchDecision,
    val isConfident: Boolean = decision == MatchDecision.AUTO_ACCEPT
)

object FaceMatcher {

    const val DUPLICATE_WARNING_THRESHOLD = 0.75f
    const val AUTO_ACCEPT_THRESHOLD = 0.65f
    const val NEED_CONFIRM_THRESHOLD = 0.55f

    /**
     * Tính Cosine Similarity giữa 2 vector đặc trưng
     * Với vector đã chuẩn hóa L2, Cosine Similarity chính là Dot Product.
     */
    fun cosineSimilarity(vec1: FloatArray, vec2: FloatArray): Float {
        if (vec1.size != vec2.size || vec1.isEmpty()) return 0f

        var dotProduct = 0f
        var normA = 0f
        var normB = 0f

        for (i in vec1.indices) {
            dotProduct += vec1[i] * vec2[i]
            normA += vec1[i] * vec1[i]
            normB += vec2[i] * vec2[i]
        }

        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator > 0) (dotProduct / denominator).coerceIn(-1f, 1f) else 0f
    }

    /**
     * Kiểm tra xem khuôn mặt mới có bị trùng lặp với sinh viên KHÁC trong database hay không
     * Tránh trường hợp chụp nhầm người hoặc một người đăng ký cho nhiều MSSV.
     */
    fun checkDuplicate(
        currentStudentId: Long,
        newEmbedding: FloatArray,
        existingRecords: List<StudentEmbeddingRecord>,
        threshold: Float = DUPLICATE_WARNING_THRESHOLD
    ): DuplicateMatch? {
        var topDuplicate: DuplicateMatch? = null
        var maxSim = threshold

        for (record in existingRecords) {
            // Chỉ kiểm tra trùng với sinh viên KHÁC (khác studentId)
            if (record.studentId != currentStudentId) {
                val sim = cosineSimilarity(newEmbedding, record.embedding)
                if (sim >= maxSim) {
                    maxSim = sim
                    topDuplicate = DuplicateMatch(
                        studentId = record.studentId,
                        mssv = record.mssv,
                        hoTen = record.hoTen,
                        similarity = sim
                    )
                }
            }
        }

        return topDuplicate
    }

    /**
     * Tìm kiếm sinh viên khớp nhất trong danh sách vector đã đăng ký
     * Áp dụng ngưỡng:
     * - Auto-accept: cosine similarity >= 0.65
     * - Need confirm: 0.55 <= similarity < 0.65
     * - Reject: similarity < 0.55
     */
    fun findBestMatch(
        faceEmbedding: FloatArray,
        studentRecords: List<StudentEmbeddingRecord>
    ): FaceMatchResult? {
        var bestMatch: StudentEmbeddingRecord? = null
        var highestScore = NEED_CONFIRM_THRESHOLD

        for (record in studentRecords) {
            val score = cosineSimilarity(faceEmbedding, record.embedding)
            if (score > highestScore) {
                highestScore = score
                bestMatch = record
            }
        }

        return bestMatch?.let {
            val decision = when {
                highestScore >= AUTO_ACCEPT_THRESHOLD -> MatchDecision.AUTO_ACCEPT
                highestScore >= NEED_CONFIRM_THRESHOLD -> MatchDecision.NEED_CONFIRM
                else -> MatchDecision.REJECT
            }

            FaceMatchResult(
                studentId = it.studentId,
                mssv = it.mssv,
                hoTen = it.hoTen,
                similarity = highestScore,
                decision = decision
            )
        }
    }
}
