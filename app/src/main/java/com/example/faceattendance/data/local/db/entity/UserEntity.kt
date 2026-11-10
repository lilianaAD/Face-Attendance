package com.example.faceattendance.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class UserRole {
    TEACHER,
    ADMIN
}

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["username"], unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val role: UserRole,
    val displayName: String,
    val email: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
