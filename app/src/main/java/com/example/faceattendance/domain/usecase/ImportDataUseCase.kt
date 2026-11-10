package com.example.faceattendance.domain.usecase

import android.net.Uri
import com.example.faceattendance.data.excel.CsvImporter
import com.example.faceattendance.data.excel.ImportResult
import com.example.faceattendance.data.local.db.entity.UserRole
import com.example.faceattendance.domain.permission.PermissionChecker
import javax.inject.Inject

enum class ImportType {
    CLASSES_AND_STUDENTS,
    COURSES,
    TEACHER_CLASS_ASSIGNMENTS
}

class ImportDataUseCase @Inject constructor(
    private val csvImporter: CsvImporter,
    private val permissionChecker: PermissionChecker
) {
    suspend operator fun invoke(
        userRole: UserRole,
        type: ImportType,
        uri: Uri
    ): Result<ImportResult> {
        return runCatching {
            permissionChecker.requireAdmin(userRole)

            when (type) {
                ImportType.CLASSES_AND_STUDENTS -> csvImporter.importClassesAndStudents(uri)
                ImportType.COURSES -> csvImporter.importCourses(uri)
                ImportType.TEACHER_CLASS_ASSIGNMENTS -> csvImporter.importTeacherClassAssignments(uri)
            }
        }
    }
}
