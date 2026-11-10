package com.example.faceattendance.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "courses",
    indices = [
        Index(value = ["courseCode"], unique = true)
    ]
)
data class CourseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val courseCode: String,
    val courseName: String,
    val credits: Int = 3
)
