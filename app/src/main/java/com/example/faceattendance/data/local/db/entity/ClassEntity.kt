package com.example.faceattendance.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "classes",
    indices = [
        Index(value = ["classCode"], unique = true)
    ]
)
data class ClassEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val classCode: String,
    val className: String,
    val description: String = ""
)
