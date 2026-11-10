package com.example.faceattendance.domain.usecase

import com.example.faceattendance.data.local.db.dao.CourseDao
import com.example.faceattendance.data.local.db.entity.CourseEntity
import com.example.faceattendance.data.local.db.entity.UserRole
import com.example.faceattendance.domain.permission.PermissionChecker
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManageCoursesUseCase @Inject constructor(
    private val courseDao: CourseDao,
    private val permissionChecker: PermissionChecker
) {
    fun getAllCourses(): Flow<List<CourseEntity>> {
        return courseDao.getAllCourses()
    }

    suspend fun createCourse(
        role: UserRole,
        courseCode: String,
        courseName: String,
        credits: Int
    ): Result<Long> = runCatching {
        permissionChecker.requireAdmin(role)

        val cleanCode = courseCode.trim().uppercase()
        val cleanName = courseName.trim()

        if (cleanCode.isEmpty()) throw IllegalArgumentException("Mã môn không được để trống")
        if (cleanName.isEmpty()) throw IllegalArgumentException("Tên môn không được để trống")
        if (credits <= 0) throw IllegalArgumentException("Số tín chỉ phải lớn hơn 0")

        val existing = courseDao.getByCode(cleanCode)
        if (existing != null) throw IllegalArgumentException("Mã môn '$cleanCode' đã tồn tại")

        courseDao.insert(
            CourseEntity(
                courseCode = cleanCode,
                courseName = cleanName,
                credits = credits
            )
        )
    }

    suspend fun updateCourse(
        role: UserRole,
        courseId: Long,
        courseName: String,
        credits: Int
    ): Result<Unit> = runCatching {
        permissionChecker.requireAdmin(role)
        val entity = courseDao.getById(courseId) ?: throw IllegalArgumentException("Không tìm thấy môn học")
        courseDao.update(entity.copy(courseName = courseName.trim(), credits = credits))
    }

    suspend fun deleteCourse(role: UserRole, course: CourseEntity): Result<Unit> = runCatching {
        permissionChecker.requireAdmin(role)
        courseDao.delete(course)
    }
}
