package com.example.faceattendance.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "face_embeddings",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["studentId"])
    ]
)
data class FaceEmbeddingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentId: Long,
    val embedding: FloatArray, // 192-D MobileFaceNet feature vector
    val imagePath: String,
    val qualityScore: Float,   // Laplacian blur or quality metric
    val pose: String = "FRONT", // FRONT, LEFT, RIGHT
    val createdAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FaceEmbeddingEntity

        if (id != other.id) return false
        if (studentId != other.studentId) return false
        if (!embedding.contentEquals(other.embedding)) return false
        if (imagePath != other.imagePath) return false
        if (qualityScore != other.qualityScore) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + studentId.hashCode()
        result = 31 * result + embedding.contentHashCode()
        result = 31 * result + imagePath.hashCode()
        result = 31 * result + qualityScore.hashCode()
        return result
    }
}
