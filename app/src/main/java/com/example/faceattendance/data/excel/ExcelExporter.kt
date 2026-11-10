package com.example.faceattendance.data.excel

import android.content.Context
import android.net.Uri
import com.example.faceattendance.data.local.db.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

sealed class ExportResult {
    data class Success(val message: String) : ExportResult()
    data class Error(val message: String) : ExportResult()
}

@Singleton
class ExcelExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Xuất báo cáo 1 buổi điểm danh
     * Sheet 1 "Điểm danh": MSSV | Họ tên | Lớp | Trạng thái | Giờ điểm danh | Độ tin cậy
     * Sheet 2 "Tổng kết": Tổng SV | Có mặt | Vắng | Tỷ lệ
     */
    suspend fun exportSessionReport(
        sessionId: Long,
        outputUri: Uri,
        db: AppDatabase
    ): ExportResult = withContext(Dispatchers.IO) {
        try {
            val session = db.sessionDao().getById(sessionId)
                ?: return@withContext ExportResult.Error("Session không tồn tại")
            val course = db.courseDao().getById(session.courseId)
            val classEntity = db.classDao().getById(session.classId)
            val students = db.studentDao().getByClassId(session.classId)
            val attendances = db.attendanceDao().getBySession(sessionId)
            val attendanceMap = attendances.associateBy { it.studentId }

            try {
                // Thử tạo file Excel với Apache POI XSSFWorkbook
                val workbook = XSSFWorkbook()

                // Sheet 1: Chi tiết
                val sheet1 = workbook.createSheet("Điểm danh")
                val header1 = sheet1.createRow(0)
                listOf("MSSV", "Họ tên", "Lớp", "Trạng thái", "Giờ điểm danh", "Độ tin cậy")
                    .forEachIndexed { i, h -> header1.createCell(i).setCellValue(h) }

                students.forEachIndexed { idx, sv ->
                    val row = sheet1.createRow(idx + 1)
                    row.createCell(0).setCellValue(sv.mssv)
                    row.createCell(1).setCellValue(sv.hoTen)
                    row.createCell(2).setCellValue(classEntity?.classCode ?: "")

                    val att = attendanceMap[sv.id]
                    if (att != null) {
                        row.createCell(3).setCellValue("Có mặt")
                        row.createCell(4).setCellValue(formatTime(att.timestamp))
                        row.createCell(5).setCellValue(String.format(Locale.US, "%.2f", att.confidence))
                    } else {
                        row.createCell(3).setCellValue("Vắng")
                        row.createCell(4).setCellValue("—")
                        row.createCell(5).setCellValue("—")
                    }
                }

                // Sheet 2: Tổng kết
                val sheet2 = workbook.createSheet("Tổng kết")
                val summaryRows = listOf(
                    "Thông tin" to "Giá trị",
                    "Môn học" to (course?.courseName ?: ""),
                    "Lớp" to (classEntity?.classCode ?: ""),
                    "Ngày" to session.sessionDate,
                    "Giờ" to "${session.startTime} - ${session.endTime}",
                    "Phòng" to session.room,
                    "Tổng sinh viên" to students.size.toString(),
                    "Có mặt" to attendances.size.toString(),
                    "Vắng" to (students.size - attendances.size).toString(),
                    "Tỷ lệ" to if (students.isEmpty()) "0%"
                    else "${(attendances.size * 100 / students.size)}%"
                )
                summaryRows.forEachIndexed { idx, (k, v) ->
                    val row = sheet2.createRow(idx)
                    row.createCell(0).setCellValue(k)
                    row.createCell(1).setCellValue(v)
                }

                // Ghi ra output
                context.contentResolver.openOutputStream(outputUri)?.use { out ->
                    workbook.write(out)
                }
                workbook.close()

                ExportResult.Success("Đã xuất báo cáo buổi học thành công (.xlsx)")
            } catch (poiError: Throwable) {
                // Fallback sang CSV (UTF-8 BOM)
                exportSessionReportCsv(outputUri, session, course?.courseName ?: "", classEntity?.classCode ?: "", students, attendanceMap)
            }
        } catch (e: Exception) {
            ExportResult.Error(e.message ?: "Lỗi xuất báo cáo buổi học")
        }
    }

    /**
     * Xuất báo cáo tổng hợp lớp theo tháng
     * Sheet 1 "Tổng hợp": MSSV | Họ tên | Số buổi có mặt | Số buổi vắng | Tỷ lệ
     * Sheet 2 "Chi tiết buổi": Ngày | Môn | Số có mặt | Số vắng
     */
    suspend fun exportClassReport(
        classId: Long,
        month: String,  // format "yyyy-MM"
        outputUri: Uri,
        db: AppDatabase
    ): ExportResult = withContext(Dispatchers.IO) {
        try {
            val classEntity = db.classDao().getById(classId)
                ?: return@withContext ExportResult.Error("Lớp không tồn tại")
            val students = db.studentDao().getByClassId(classId)
            val sessions = db.sessionDao().getByClassAndMonth(classId, month)
            val allAttendances = sessions.flatMap { db.attendanceDao().getBySession(it.id) }
            val attendanceByStudent = allAttendances.groupBy { it.studentId }

            try {
                val workbook = XSSFWorkbook()

                // Sheet 1: Tổng hợp
                val sheet1 = workbook.createSheet("Tổng hợp")
                val header1 = sheet1.createRow(0)
                listOf("MSSV", "Họ tên", "Số buổi có mặt", "Số buổi vắng", "Tỷ lệ", "Cảnh báo")
                    .forEachIndexed { i, h -> header1.createCell(i).setCellValue(h) }

                students.forEachIndexed { idx, sv ->
                    val row = sheet1.createRow(idx + 1)
                    val attended = attendanceByStudent[sv.id]?.size ?: 0
                    val absent = sessions.size - attended
                    val rate = if (sessions.isEmpty()) 0 else (attended * 100 / sessions.size)
                    val isDanger = sessions.isNotEmpty() && ((absent.toFloat() / sessions.size) >= 0.20f)

                    row.createCell(0).setCellValue(sv.mssv)
                    row.createCell(1).setCellValue(sv.hoTen)
                    row.createCell(2).setCellValue(attended.toDouble())
                    row.createCell(3).setCellValue(absent.toDouble())
                    row.createCell(4).setCellValue("$rate%")
                    row.createCell(5).setCellValue(if (isDanger) "Cảnh báo cấm thi (vắng >= 20%)" else "Bình thường")
                }

                // Sheet 2: Chi tiết buổi
                val sheet2 = workbook.createSheet("Chi tiết buổi")
                val header2 = sheet2.createRow(0)
                listOf("Ngày", "Môn", "Giờ", "Số có mặt", "Số vắng", "Tỷ lệ")
                    .forEachIndexed { i, h -> header2.createCell(i).setCellValue(h) }

                sessions.forEachIndexed { idx, s ->
                    val row = sheet2.createRow(idx + 1)
                    val course = db.courseDao().getById(s.courseId)
                    val att = db.attendanceDao().getBySession(s.id)
                    row.createCell(0).setCellValue(s.sessionDate)
                    row.createCell(1).setCellValue(course?.courseName ?: "")
                    row.createCell(2).setCellValue("${s.startTime} - ${s.endTime}")
                    row.createCell(3).setCellValue(att.size.toDouble())
                    row.createCell(4).setCellValue((students.size - att.size).toDouble())
                    row.createCell(5).setCellValue(
                        if (students.isEmpty()) "0%" else "${att.size * 100 / students.size}%"
                    )
                }

                context.contentResolver.openOutputStream(outputUri)?.use { out ->
                    workbook.write(out)
                }
                workbook.close()

                ExportResult.Success("Đã xuất báo cáo lớp ${classEntity.classCode} thành công (.xlsx)")
            } catch (poiError: Throwable) {
                // Fallback sang CSV
                exportClassReportCsv(outputUri, classEntity.classCode, month, students, sessions, db)
            }
        } catch (e: Exception) {
            ExportResult.Error(e.message ?: "Lỗi xuất báo cáo lớp học")
        }
    }

    private fun exportSessionReportCsv(
        outputUri: Uri,
        session: com.example.faceattendance.data.local.db.entity.SessionEntity,
        courseName: String,
        classCode: String,
        students: List<com.example.faceattendance.data.local.db.entity.StudentEntity>,
        attendanceMap: Map<Long, com.example.faceattendance.data.local.db.entity.AttendanceEntity>
    ): ExportResult {
        context.contentResolver.openOutputStream(outputUri)?.use { stream ->
            OutputStreamWriter(stream, StandardCharsets.UTF_8).use { writer ->
                // Write UTF-8 BOM so Excel opens Vietnamese characters cleanly
                writer.write("\uFEFF")
                writer.write("--- BÁO CÁO ĐIỂM DANH BUỔI HỌC ---\n")
                writer.write("Môn học: $courseName, Lớp: $classCode, Ngày: ${session.sessionDate}, Phòng: ${session.room}\n\n")
                writer.write("MSSV,Họ tên,Lớp,Trạng thái,Giờ điểm danh,Độ tin cậy\n")
                students.forEach { sv ->
                    val att = attendanceMap[sv.id]
                    val status = if (att != null) "Có mặt" else "Vắng"
                    val time = if (att != null) formatTime(att.timestamp) else "—"
                    val conf = if (att != null) String.format(Locale.US, "%.2f", att.confidence) else "—"
                    writer.write("\"${sv.mssv}\",\"${sv.hoTen}\",\"$classCode\",\"$status\",\"$time\",\"$conf\"\n")
                }
            }
        }
        return ExportResult.Success("Đã xuất báo cáo buổi học định dạng CSV (UTF-8 BOM)")
    }

    private suspend fun exportClassReportCsv(
        outputUri: Uri,
        classCode: String,
        month: String,
        students: List<com.example.faceattendance.data.local.db.entity.StudentEntity>,
        sessions: List<com.example.faceattendance.data.local.db.entity.SessionEntity>,
        db: AppDatabase
    ): ExportResult {
        val allAttendances = sessions.flatMap { db.attendanceDao().getBySession(it.id) }
        val attendanceByStudent = allAttendances.groupBy { it.studentId }

        context.contentResolver.openOutputStream(outputUri)?.use { stream ->
            OutputStreamWriter(stream, StandardCharsets.UTF_8).use { writer ->
                writer.write("\uFEFF")
                writer.write("--- BÁO CÁO TỔNG HỢP CHUYÊN CẦN THÁNG $month ---\n")
                writer.write("Lớp: $classCode, Tổng số buổi học: ${sessions.size}\n\n")
                writer.write("MSSV,Họ tên,Số buổi có mặt,Số buổi vắng,Tỷ lệ,Cảnh báo\n")
                students.forEach { sv ->
                    val attended = attendanceByStudent[sv.id]?.size ?: 0
                    val absent = sessions.size - attended
                    val rate = if (sessions.isEmpty()) 0 else (attended * 100 / sessions.size)
                    val warning = if (sessions.isNotEmpty() && (absent.toFloat() / sessions.size >= 0.20f)) "CẢNH BÁO CẤM THI" else "Đạt"
                    writer.write("\"${sv.mssv}\",\"${sv.hoTen}\",$attended,$absent,\"$rate%\",\"$warning\"\n")
                }
            }
        }
        return ExportResult.Success("Đã xuất báo cáo lớp định dạng CSV (UTF-8 BOM)")
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
