package com.example.faceattendance.presentation.admin.reports

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.faceattendance.data.excel.ExcelExporter
import com.example.faceattendance.data.excel.ExportResult
import com.example.faceattendance.data.local.db.AppDatabase
import com.example.faceattendance.data.local.db.entity.ClassEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class ClassReportItem(
    val classEntity: ClassEntity,
    val totalStudents: Int,
    val totalSessions: Int,
    val averageRate: Float,
    val dangerCount: Int
)

data class AdminReportsUiState(
    val classReports: List<ClassReportItem> = emptyList(),
    val totalStudentsCount: Int = 0,
    val totalClassesCount: Int = 0,
    val totalSessionsCount: Int = 0,
    val overallAttendanceRate: Float = 0f,
    val totalDangerCount: Int = 0,
    val currentMonth: String = "",
    val isLoading: Boolean = false,
    val exportResult: ExportResult? = null
)

@HiltViewModel
class AdminReportsViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
    private val excelExporter: ExcelExporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminReportsUiState())
    val uiState: StateFlow<AdminReportsUiState> = _uiState.asStateFlow()

    init {
        val month = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        _uiState.update { it.copy(currentMonth = month) }
        loadSchoolStats(month)
    }

    fun loadSchoolStats(month: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, currentMonth = month) }

            val classes = appDatabase.classDao().getAllClassesSync()
            val allSessions = appDatabase.sessionDao().getAllSessions()
            val allAttendances = appDatabase.attendanceDao().getAll()

            val classReportItems = classes.map { cl ->
                val students = appDatabase.studentDao().getByClassId(cl.id)
                val classSessions = allSessions.filter { it.classId == cl.id && it.sessionDate.startsWith(month) }
                val sessionIds = classSessions.map { it.id }.toSet()
                val classAttendances = allAttendances.filter { it.sessionId in sessionIds }
                val attByStudent = classAttendances.groupBy { it.studentId }

                val totalSessions = classSessions.size
                var danger = 0
                val rates = students.map { s ->
                    val attended = attByStudent[s.id]?.size ?: 0
                    val absent = totalSessions - attended
                    if (totalSessions > 0 && (absent.toFloat() / totalSessions >= 0.20f)) {
                        danger++
                    }
                    if (totalSessions > 0) (attended.toFloat() / totalSessions) * 100f else 0f
                }

                val avgRate = if (rates.isNotEmpty()) rates.average().toFloat() else 0f

                ClassReportItem(
                    classEntity = cl,
                    totalStudents = students.size,
                    totalSessions = totalSessions,
                    averageRate = avgRate,
                    dangerCount = danger
                )
            }

            val totalStudents = classReportItems.sumOf { it.totalStudents }
            val totalDanger = classReportItems.sumOf { it.dangerCount }
            val overallRate = if (classReportItems.isNotEmpty()) {
                classReportItems.map { it.averageRate }.average().toFloat()
            } else 0f

            _uiState.update {
                it.copy(
                    classReports = classReportItems,
                    totalStudentsCount = totalStudents,
                    totalClassesCount = classes.size,
                    totalSessionsCount = allSessions.size,
                    overallAttendanceRate = overallRate,
                    totalDangerCount = totalDanger,
                    isLoading = false
                )
            }
        }
    }

    fun exportClassReport(classId: Long, outputUri: Uri) {
        viewModelScope.launch {
            val res = excelExporter.exportClassReport(classId, _uiState.value.currentMonth, outputUri, appDatabase)
            _uiState.update { it.copy(exportResult = res) }
        }
    }

    fun clearExportResult() {
        _uiState.update { it.copy(exportResult = null) }
    }
}
