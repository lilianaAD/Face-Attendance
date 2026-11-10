package com.example.faceattendance.domain.usecase

import com.example.faceattendance.data.local.db.dao.ClassDao
import com.example.faceattendance.data.local.db.dao.StudentWithFaceInfo
import com.example.faceattendance.data.local.db.entity.ClassEntity
import com.example.faceattendance.data.local.db.entity.StudentEntity
import com.example.faceattendance.data.repository.StudentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTeacherStudentsUseCase @Inject constructor(
    private val classDao: ClassDao,
    private val studentRepository: StudentRepository
) {
    fun getClassesForTeacher(teacherId: Long): Flow<List<ClassEntity>> {
        return classDao.getClassesForTeacher(teacherId)
    }

    fun getStudentsWithFaceInfo(classId: Long): Flow<List<StudentWithFaceInfo>> {
        return studentRepository.getStudentsWithFaceInfoByClass(classId)
    }

    suspend fun getStudentById(studentId: Long): StudentEntity? {
        return studentRepository.getStudentById(studentId)
    }

    suspend fun countMissingFace(classId: Long): Int {
        return studentRepository.countStudentsWithoutFaceByClass(classId)
    }
}
