package com.example.faceattendance.domain.usecase

import com.example.faceattendance.data.local.db.dao.ClassDao
import com.example.faceattendance.data.local.db.entity.ClassEntity
import com.example.faceattendance.data.local.db.entity.UserRole
import com.example.faceattendance.domain.permission.PermissionChecker
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManageClassesUseCase @Inject constructor(
    private val classDao: ClassDao,
    private val permissionChecker: PermissionChecker
) {
    fun getAllClasses(): Flow<List<ClassEntity>> {
        return classDao.getAllClasses()
    }

    suspend fun createClass(
        role: UserRole,
        classCode: String,
        className: String,
        description: String = ""
    ): Result<Long> = runCatching {
        permissionChecker.requireAdmin(role)

        val cleanCode = classCode.trim().uppercase()
        val cleanName = className.trim()

        if (cleanCode.isEmpty()) throw IllegalArgumentException("Mã lớp không được để trống")
        if (cleanName.isEmpty()) throw IllegalArgumentException("Tên lớp không được để trống")

        val existing = classDao.getByCode(cleanCode)
        if (existing != null) throw IllegalArgumentException("Mã lớp '$cleanCode' đã tồn tại")

        classDao.insert(
            ClassEntity(
                classCode = cleanCode,
                className = cleanName,
                description = description.trim()
            )
        )
    }

    suspend fun updateClass(
        role: UserRole,
        classId: Long,
        className: String,
        description: String
    ): Result<Unit> = runCatching {
        permissionChecker.requireAdmin(role)
        val entity = classDao.getById(classId) ?: throw IllegalArgumentException("Không tìm thấy lớp học")
        classDao.update(entity.copy(className = className.trim(), description = description.trim()))
    }

    suspend fun deleteClass(role: UserRole, classEntity: ClassEntity): Result<Unit> = runCatching {
        permissionChecker.requireAdmin(role)
        classDao.delete(classEntity)
    }
}
