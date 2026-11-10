package com.example.faceattendance.domain.usecase

import com.example.faceattendance.data.local.db.dao.TeacherClassDao
import com.example.faceattendance.data.local.db.entity.TeacherClassEntity
import com.example.faceattendance.data.local.db.entity.UserRole
import com.example.faceattendance.domain.permission.PermissionChecker
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManageAssignmentsUseCase @Inject constructor(
    private val teacherClassDao: TeacherClassDao,
    private val permissionChecker: PermissionChecker
) {
    fun getAssignedClassIdsForTeacher(teacherId: Long): Flow<List<Long>> {
        return teacherClassDao.getClassIdsForTeacher(teacherId)
    }

    fun getAssignedTeacherIdsForClass(classId: Long): Flow<List<Long>> {
        return teacherClassDao.getTeacherIdsForClass(classId)
    }

    suspend fun assignTeacherToClass(
        role: UserRole,
        teacherId: Long,
        classId: Long
    ): Result<Unit> = runCatching {
        permissionChecker.requireAdmin(role)
        teacherClassDao.insert(TeacherClassEntity(teacherId = teacherId, classId = classId))
    }

    suspend fun removeAssignment(
        role: UserRole,
        teacherId: Long,
        classId: Long
    ): Result<Unit> = runCatching {
        permissionChecker.requireAdmin(role)
        teacherClassDao.removeAssignment(teacherId, classId)
    }
}
