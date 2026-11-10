package com.example.faceattendance.domain.permission

import com.example.faceattendance.data.local.db.dao.SessionDao
import com.example.faceattendance.data.local.db.dao.TeacherClassDao
import com.example.faceattendance.data.local.db.entity.UserRole
import javax.inject.Inject
import javax.inject.Singleton

class UnauthorizedException(message: String) : SecurityException(message)

@Singleton
class PermissionChecker @Inject constructor(
    private val teacherClassDao: TeacherClassDao,
    private val sessionDao: SessionDao
) {

    fun requireAdmin(role: UserRole?) {
        if (role != UserRole.ADMIN) {
            throw UnauthorizedException("Hành động này chỉ dành riêng cho Quản trị viên (ADMIN).")
        }
    }

    fun requireTeacherOrAdmin(role: UserRole?) {
        if (role != UserRole.TEACHER && role != UserRole.ADMIN) {
            throw UnauthorizedException("Bạn không có quyền truy cập chức năng này.")
        }
    }

    suspend fun canTeacherAccessClass(userId: Long, role: UserRole, classId: Long): Boolean {
        if (role == UserRole.ADMIN) return true
        if (role == UserRole.TEACHER) {
            val count = teacherClassDao.isTeacherAssignedToClass(userId, classId)
            return count > 0
        }
        return false
    }

    suspend fun canTeacherAccessSession(userId: Long, role: UserRole, sessionId: Long): Boolean {
        if (role == UserRole.ADMIN) return true
        if (role == UserRole.TEACHER) {
            val session = sessionDao.getById(sessionId) ?: return false
            return canTeacherAccessClass(userId, role, session.classId)
        }
        return false
    }

    suspend fun verifySessionAccess(userId: Long, role: UserRole, sessionId: Long) {
        if (!canTeacherAccessSession(userId, role, sessionId)) {
            throw UnauthorizedException("Bạn không phụ trách lớp học của buổi dạy này.")
        }
    }

    suspend fun verifyClassAccess(userId: Long, role: UserRole, classId: Long) {
        if (!canTeacherAccessClass(userId, role, classId)) {
            throw UnauthorizedException("Bạn không được phân công giảng dạy lớp này.")
        }
    }
}
