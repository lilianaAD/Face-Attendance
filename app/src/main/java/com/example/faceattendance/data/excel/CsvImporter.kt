package com.example.faceattendance.data.excel

import android.content.Context
import android.net.Uri
import com.example.faceattendance.data.local.db.AppDatabase
import com.example.faceattendance.data.local.db.entity.ClassEntity
import com.example.faceattendance.data.local.db.entity.CourseEntity
import com.example.faceattendance.data.local.db.entity.StudentEntity
import com.example.faceattendance.data.local.db.entity.TeacherClassEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

data class ImportError(
    val rowNumber: Int,
    val message: String
)

data class ImportResult(
    val totalRows: Int,
    val added: Int,
    val skipped: Int,
    val errors: List<ImportError>
)

@Singleton
class CsvImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase
) {
    /**
     * Import lớp + sinh viên từ file CSV
     * Format: MSSV,Ho Ten,Lop,Email (dòng đầu là header, bỏ qua)
     */
    suspend fun importClassesAndStudents(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val studentDao = database.studentDao()
        val classDao = database.classDao()

        var totalRows = 0
        var added = 0
        var skipped = 0
        val errors = mutableListOf<ImportError>()

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    var line: String?
                    var rowNumber = 0

                    while (reader.readLine().also { line = it } != null) {
                        rowNumber++
                        val currentLine = line?.trim() ?: continue
                        if (currentLine.isEmpty()) continue

                        // Bỏ qua dòng tiêu đề đầu tiên
                        if (rowNumber == 1) continue

                        totalRows++
                        val tokens = parseCsvLine(currentLine)

                        if (tokens.size < 3) {
                            errors.add(ImportError(rowNumber, "Dòng không đủ số cột yêu cầu (cần tối thiểu MSSV, Họ tên, Lớp)"))
                            continue
                        }

                        val mssv = tokens[0].trim()
                        val hoTen = tokens[1].trim()
                        val lopCode = tokens[2].trim()
                        val email = if (tokens.size > 3) tokens[3].trim() else ""

                        if (mssv.isEmpty() || hoTen.isEmpty() || lopCode.isEmpty()) {
                            errors.add(ImportError(rowNumber, "MSSV, Họ tên và Mã lớp không được để trống"))
                            continue
                        }

                        // 1. Kiểm tra hoặc tự động tạo Lớp nếu chưa tồn tại
                        var classEntity = classDao.getByCode(lopCode)
                        if (classEntity == null) {
                            val newClassId = classDao.insert(
                                ClassEntity(
                                    classCode = lopCode,
                                    className = "Lớp $lopCode",
                                    description = "Tự động tạo từ quá trình import sinh viên"
                                )
                            )
                            classEntity = classDao.getById(newClassId) ?: classDao.getByCode(lopCode)
                        }

                        if (classEntity == null) {
                            errors.add(ImportError(rowNumber, "Không thể khởi tạo lớp học cho mã '$lopCode'"))
                            continue
                        }

                        // 2. Chống trùng MSSV (INSERT OR IGNORE)
                        val existingStudent = studentDao.getByMssv(mssv)
                        if (existingStudent != null) {
                            skipped++
                            continue
                        }

                        val studentId = studentDao.insert(
                            StudentEntity(
                                mssv = mssv,
                                hoTen = hoTen,
                                classId = classEntity.id,
                                email = email
                            )
                        )

                        if (studentId > 0) {
                            added++
                        } else {
                            skipped++
                        }
                    }
                }
            } ?: return@withContext ImportResult(0, 0, 0, listOf(ImportError(0, "Không thể mở file dữ liệu đã chọn")))
        } catch (e: Exception) {
            errors.add(ImportError(0, "Lỗi đọc file: ${e.localizedMessage ?: e.message}"))
        }

        ImportResult(totalRows, added, skipped, errors)
    }

    /**
     * Import môn học từ file CSV
     * Format: Ma Mon,Ten Mon,So Tin Chi (dòng đầu là header, bỏ qua)
     */
    suspend fun importCourses(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val courseDao = database.courseDao()

        var totalRows = 0
        var added = 0
        var skipped = 0
        val errors = mutableListOf<ImportError>()

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    var line: String?
                    var rowNumber = 0

                    while (reader.readLine().also { line = it } != null) {
                        rowNumber++
                        val currentLine = line?.trim() ?: continue
                        if (currentLine.isEmpty()) continue

                        // Bỏ qua dòng tiêu đề
                        if (rowNumber == 1) continue

                        totalRows++
                        val tokens = parseCsvLine(currentLine)

                        if (tokens.size < 3) {
                            errors.add(ImportError(rowNumber, "Dòng không đủ 3 cột (Mã môn, Tên môn, Số tín chỉ)"))
                            continue
                        }

                        val courseCode = tokens[0].trim()
                        val courseName = tokens[1].trim()
                        val creditsStr = tokens[2].trim()

                        if (courseCode.isEmpty() || courseName.isEmpty()) {
                            errors.add(ImportError(rowNumber, "Mã môn và Tên môn không được để trống"))
                            continue
                        }

                        val credits = creditsStr.toIntOrNull()
                        if (credits == null || credits <= 0) {
                            errors.add(ImportError(rowNumber, "Số tín chỉ '$creditsStr' phải là số nguyên dương lớn hơn 0"))
                            continue
                        }

                        // Kiểm tra trùng mã môn
                        val existing = courseDao.getByCode(courseCode)
                        if (existing != null) {
                            skipped++
                            continue
                        }

                        val insertId = courseDao.insert(
                            CourseEntity(
                                courseCode = courseCode,
                                courseName = courseName,
                                credits = credits
                            )
                        )

                        if (insertId > 0) {
                            added++
                        } else {
                            skipped++
                        }
                    }
                }
            } ?: return@withContext ImportResult(0, 0, 0, listOf(ImportError(0, "Không thể mở file dữ liệu đã chọn")))
        } catch (e: Exception) {
            errors.add(ImportError(0, "Lỗi đọc file môn học: ${e.localizedMessage ?: e.message}"))
        }

        ImportResult(totalRows, added, skipped, errors)
    }

    /**
     * Import gán GV-lớp từ file CSV
     * Format: Username GV,Ma Lop (dòng đầu là header, bỏ qua)
     */
    suspend fun importTeacherClassAssignments(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val userDao = database.userDao()
        val classDao = database.classDao()
        val teacherClassDao = database.teacherClassDao()

        var totalRows = 0
        var added = 0
        var skipped = 0
        val errors = mutableListOf<ImportError>()

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    var line: String?
                    var rowNumber = 0

                    while (reader.readLine().also { line = it } != null) {
                        rowNumber++
                        val currentLine = line?.trim() ?: continue
                        if (currentLine.isEmpty()) continue

                        // Bỏ qua dòng tiêu đề
                        if (rowNumber == 1) continue

                        totalRows++
                        val tokens = parseCsvLine(currentLine)

                        if (tokens.size < 2) {
                            errors.add(ImportError(rowNumber, "Dòng không đủ 2 cột (Username GV, Mã Lớp)"))
                            continue
                        }

                        val username = tokens[0].trim()
                        val classCode = tokens[1].trim()

                        if (username.isEmpty() || classCode.isEmpty()) {
                            errors.add(ImportError(rowNumber, "Username và Mã lớp không được để trống"))
                            continue
                        }

                        val teacher = userDao.getByUsername(username)
                        if (teacher == null) {
                            errors.add(ImportError(rowNumber, "Không tìm thấy giáo viên với username '$username'"))
                            continue
                        }

                        val classEntity = classDao.getByCode(classCode)
                        if (classEntity == null) {
                            errors.add(ImportError(rowNumber, "Không tìm thấy lớp học với mã '$classCode'"))
                            continue
                        }

                        val isAlreadyAssigned = teacherClassDao.isTeacherAssignedToClass(teacher.id, classEntity.id) > 0
                        if (isAlreadyAssigned) {
                            skipped++
                            continue
                        }

                        teacherClassDao.insert(
                            TeacherClassEntity(
                                teacherId = teacher.id,
                                classId = classEntity.id
                            )
                        )
                        added++
                    }
                }
            } ?: return@withContext ImportResult(0, 0, 0, listOf(ImportError(0, "Không thể mở file dữ liệu đã chọn")))
        } catch (e: Exception) {
            errors.add(ImportError(0, "Lỗi đọc file phân công: ${e.localizedMessage ?: e.message}"))
        }

        ImportResult(totalRows, added, skipped, errors)
    }

    /**
     * Tách dòng CSV hỗ trợ trường hợp có dấu ngoặc kép chứa dấu phẩy
     */
    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var insideQuotes = false

        for (ch in line) {
            when (ch) {
                '"' -> insideQuotes = !insideQuotes
                ',' -> {
                    if (insideQuotes) {
                        sb.append(ch)
                    } else {
                        tokens.add(sb.toString().trim().removeSurrounding("\""))
                        sb.clear()
                    }
                }
                else -> sb.append(ch)
            }
        }
        tokens.add(sb.toString().trim().removeSurrounding("\""))
        return tokens
    }
}
