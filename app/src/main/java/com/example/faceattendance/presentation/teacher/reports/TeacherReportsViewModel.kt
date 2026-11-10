package com.example.faceattendance.presentation.teacher.reports

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.faceattendance.data.excel.ExcelExporter
import com.example.faceattendance.data.excel.ExportResult
import com.example.faceattendance.data.local.db.AppDatabase
import com.example.faceattendance.data.local.db.entity.ClassEntity
import com.example.faceattendance.data.local.db.entity.SessionEntity
import com.example.faceattendance.data.local.preferences.AuthDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class StudentAttendanceStat(
    val studentId: Long,
    val mssv: String,
    val hoTen: String,
    val attendedSessions: Int,
    val absentSessions: Int,
    val attendanceRate: Float,
    val isDanger: Boolean // vắng >= 20%
)

data class TeacherReportsUiState(
    val classes: List<ClassEntity> = emptyList(),
    val selectedClassId: Long? = null,
    val selectedMonth: String = "", // format yyyy-MM
    val sessions: List<SessionEntity> = emptyList(),
    val studentStats: List<StudentAttendanceStat> = emptyList(),
    val totalSessionsCount: Int = 0,
    val averageRate: Float = 0f,
    val dangerCount: Int = 0,
    val isLoading: Boolean = false,
    val exportResult: ExportResult? = null
)

@HiltViewModel
class TeacherReportsViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
    private val authDataStore: AuthDataStore,
    private val excelExporter: ExcelExporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherReportsUiState())
    val uiState: StateFlow<TeacherReportsUiState> = _uiState.asStateFlow()

    init {
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        _uiState.update { it.copy(selectedMonth = currentMonth) }
        loadClasses()
    }

    private fun loadClasses() {
        viewModelScope.launch {
            val user = authDataStore.currentUser.firstOrNull() ?: return@launch
            appDatabase.classDao().getClassesForTeacher(user.id).collect { classList ->
                _uiState.update { 
                    it.copy(
                        classes = classList,
                        selectedClassId = it.selectedClassId ?: classList.firstOrNull()?.id
                    ) 
                }
                _uiState.value.selectedClassId?.let { classId ->
                    loadStats(classId, _uiState.value.selectedMonth)
                }
            }
        }
    }

    fun selectClass(classId: Long) {
        _uiState.update { it.copy(selectedClassId = classId) }
        loadStats(classId, _uiState.value.selectedMonth)
    }

    fun selectMonth(month: String) {
        _uiState.update { it.copy(selectedMonth = month) }
        _uiState.value.selectedClassId?.let { classId ->
            loadStats(classId, month)
        }
    }

    private fun loadStats(classId: Long, month: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val sessions = appDatabase.sessionDao().getByClassAndMonth(classId, month)
            val students = appDatabase.studentDao().getByClassId(classId)
            val sessionIds = sessions.map { it.id }
            val attendances = if (sessionIds.isNotEmpty()) {
                appDatabase.attendanceDao().getBySessionIds(sessionIds)
            } else {
                emptyList()
            }

            val attendanceByStudent = attendances.groupBy { it.studentId }
            val totalSessions = sessions.size

            val stats = students.map { sv ->
                val attended = attendanceByStudent[sv.id]?.size ?: 0
                val absent = totalSessions - attended
                val rate = if (totalSessions > 0) (attended.toFloat() / totalSessions) * 100f else 0f
                val isDanger = totalSessions > 0 && ((absent.toFloat() / totalSessions) >= 0.20f)

                StudentAttendanceStat(
                    studentId = sv.id,
                    mssv = sv.mssv,
                    hoTen = sv.hoTen,
                    attendedSessions = attended,
                    absentSessions = absent,
                    attendanceRate = rate,
                    isDanger = isDanger
                )
            }

            val avg = if (stats.isNotEmpty() && totalSessions > 0) {
                stats.map { it.attendanceRate }.average().toFloat()
            } else 0f

            val danger = stats.count { it.isDanger }

            _uiState.update {
                it.copy(
                    sessions = sessions,
                    studentStats = stats,
                    totalSessionsCount = totalSessions,
                    averageRate = avg,
                    dangerCount = danger,
                    isLoading = false
                )
            }
        }
    }

    fun exportClassReport(outputUri: Uri) {
        val classId = _uiState.value.selectedClassId ?: return
        val month = _uiState.value.selectedMonth
        viewModelScope.launch {
            val result = excelExporter.exportClassReport(classId, month, outputUri, appDatabase)
            _uiState.update { it.copy(exportResult = result) }
        }
    }

    fun clearExportResult() {
        _uiState.update { it.copy(exportResult = null) }
    }
}
