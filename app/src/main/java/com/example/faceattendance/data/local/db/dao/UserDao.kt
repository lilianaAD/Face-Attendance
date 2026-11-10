package com.example.faceattendance.data.local.db.dao

import androidx.room.*
import com.example.faceattendance.data.local.db.entity.UserEntity
import com.example.faceattendance.data.local.db.entity.UserRole
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(user: UserEntity): Long

    @Update
    suspend fun update(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE role = :role ORDER BY displayName ASC")
    fun getUsersByRole(role: UserRole): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    suspend fun getAllUsersSync(): List<UserEntity>

    @Query("UPDATE users SET passwordHash = :newHash WHERE id = :userId")
    suspend fun updatePassword(userId: Long, newHash: String)

    @Query("UPDATE users SET isActive = :isActive WHERE id = :userId")
    suspend fun setActiveStatus(userId: Long, isActive: Boolean)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int
}
