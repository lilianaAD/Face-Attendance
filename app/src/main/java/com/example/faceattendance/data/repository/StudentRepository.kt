package com.example.faceattendance.data.repository

import com.example.faceattendance.data.local.db.dao.AttendanceDao
import com.example.faceattendance.data.local.db.dao.AttendanceRecordItem
import com.example.faceattendance.data.local.db.dao.StudentDao
import com.example.faceattendance.data.local.db.dao.StudentWithFaceInfo
import com.example.faceattendance.data.local.db.entity.AttendanceEntity
import com.example.faceattendance.data.local.db.entity.StudentEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface StudentRepository {
    fun getStudentsByClass(classId: Long): Flow<List<StudentEntity>>
    fun getStudentsWithFaceInfoByClass(classId: Long): Flow<List<StudentWithFaceInfo>>
    suspend fun getStudentById(studentId: Long): StudentEntity?
    suspend fun countStudentsWithoutFaceByClass(classId: Long): Int
    fun getAttendancesForSession(sessionId: Long): Flow<List<AttendanceRecordItem>>
    fun getAttendancesForStudent(studentId: Long): Flow<List<AttendanceEntity>>
}

@Singleton
class StudentRepositoryImpl @Inject constructor(
    private val studentDao: StudentDao,
    private val attendanceDao: AttendanceDao
) : StudentRepository {

    override fun getStudentsByClass(classId: Long): Flow<List<StudentEntity>> {
        return studentDao.getStudentsByClass(classId)
    }

    override fun getStudentsWithFaceInfoByClass(classId: Long): Flow<List<StudentWithFaceInfo>> {
        return studentDao.getStudentsWithFaceInfoByClass(classId)
    }

    override suspend fun getStudentById(studentId: Long): StudentEntity? {
        return studentDao.getById(studentId)
    }

    override suspend fun countStudentsWithoutFaceByClass(classId: Long): Int {
        return studentDao.countStudentsWithoutFaceByClass(classId)
    }

    override fun getAttendancesForSession(sessionId: Long): Flow<List<AttendanceRecordItem>> {
        return attendanceDao.getAttendancesForSession(sessionId)
    }

    override fun getAttendancesForStudent(studentId: Long): Flow<List<AttendanceEntity>> {
        return attendanceDao.getAttendancesForStudent(studentId)
    }
}
