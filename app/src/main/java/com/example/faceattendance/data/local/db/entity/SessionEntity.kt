package com.example.faceattendance.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SessionStatus {
    SCHEDULED,
    OPEN,
    CLOSED
}

@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(
            value = ["courseId", "classId", "sessionDate", "startTime"],
            unique = true
        ),
        Index(value = ["classId"]),
        Index(value = ["courseId"])
    ]
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val courseId: Long,
    val classId: Long,
    val sessionDate: String, // YYYY-MM-DD
    val startTime: String,   // HH:mm
    val endTime: String,     // HH:mm
    val room: String,
    val status: SessionStatus = SessionStatus.SCHEDULED,
    val createdAt: Long = System.currentTimeMillis()
)
