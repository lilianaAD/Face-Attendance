package com.example.faceattendance.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AttendanceMethod {
    FACE,
    FACE_LOW_CONF,
    MANUAL
}

@Entity(
    tableName = "attendances",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(
            value = ["sessionId", "studentId"],
            unique = true
        ),
        Index(value = ["sessionId"]),
        Index(value = ["studentId"])
    ]
)
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val studentId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val confidence: Float = 1.0f,
    val method: AttendanceMethod,
    val imagePath: String? = null,
    val note: String? = null
)
