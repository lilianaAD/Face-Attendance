package com.example.faceattendance.presentation.teacher.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.example.faceattendance.data.excel.ExcelExporter
import com.example.faceattendance.data.excel.ExportResult
import com.example.faceattendance.data.local.db.AppDatabase
import com.example.faceattendance.data.local.db.dao.AttendanceRecordItem
import com.example.faceattendance.data.local.db.dao.SessionWithDetails
import com.example.faceattendance.data.local.db.dao.StudentWithFaceInfo
import com.example.faceattendance.domain.usecase.GetSessionDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionStudentRow(
    val studentId: Long,
    val mssv: String,
    val hoTen: String,
    val faceCount: Int,
    val isAttended: Boolean,
    val method: String? = null,
    val time: String? = null
)

data class SessionDetailUiState(
    val sessionDetails: SessionWithDetails? = null,
    val studentRows: List<SessionStudentRow> = emptyList(),
    val totalStudents: Int = 0,
    val attendedCount: Int = 0,
    val attendanceRate: Float = 0f,
    val isOpen: Boolean = true,
    val isLoading: Boolean = true,
    val showSummaryDialog: Boolean = false,
    val exportResult: ExportResult? = null,
    val message: String? = null
)

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getSessionDetailsUseCase: GetSessionDetailsUseCase,
    private val excelExporter: ExcelExporter,
    private val appDatabase: AppDatabase
) : ViewModel() {

    private val sessionId: Long = savedStateHandle.get<Long>("sessionId") ?: 1L

    private val _uiState = MutableStateFlow(SessionDetailUiState())
    val uiState: StateFlow<SessionDetailUiState> = _uiState.asStateFlow()

    init {
        loadSessionData()
    }

    private fun loadSessionData() {
        viewModelScope.launch {
            val details = getSessionDetailsUseCase.getDetails(sessionId)
            if (details == null) {
                _uiState.update { it.copy(isLoading = false, message = "Không tìm thấy buổi học") }
                return@launch
            }

            _uiState.update {
                it.copy(
                    sessionDetails = details,
                    isOpen = details.session.status.name == "OPEN",
                    isLoading = false
                )
            }

            // Combine students and attendances
            combine(
                getSessionDetailsUseCase.getStudentsWithFaceInfo(details.session.classId),
                getSessionDetailsUseCase.getSessionAttendances(sessionId)
            ) { students, attendances ->
                val rows = students.map { s ->
                    val attendance = attendances.find { it.attendance.studentId == s.student.id }
                    SessionStudentRow(
                        studentId = s.student.id,
                        mssv = s.student.mssv,
                        hoTen = s.student.hoTen,
                        faceCount = s.faceCount,
                        isAttended = attendance != null,
                        method = attendance?.attendance?.method?.name,
                        time = attendance?.attendance?.timestamp?.let { "Đã điểm danh" }
                    )
                }

                val total = rows.size
                val attended = rows.count { it.isAttended }
                val rate = if (total > 0) (attended.toFloat() / total) * 100f else 0f

                _uiState.update {
                    it.copy(
                        studentRows = rows,
                        totalStudents = total,
                        attendedCount = attended,
                        attendanceRate = rate
                    )
                }
            }.collect()
        }
    }

    fun toggleSessionStatus() {
        val currentStatus = _uiState.value.isOpen
        viewModelScope.launch {
            val result = getSessionDetailsUseCase.toggleSessionStatus(sessionId, !currentStatus)
            result.onSuccess {
                _uiState.update { 
                    it.copy(
                        isOpen = !currentStatus,
                        // If transitioning from OPEN to CLOSED, show summary dialog
                        showSummaryDialog = currentStatus
                    ) 
                }
            }
        }
    }

    fun openSummaryDialog() {
        _uiState.update { it.copy(showSummaryDialog = true) }
    }

    fun dismissSummaryDialog() {
        _uiState.update { it.copy(showSummaryDialog = false) }
    }

    fun exportSessionExcel(outputUri: Uri) {
        viewModelScope.launch {
            val res = excelExporter.exportSessionReport(sessionId, outputUri, appDatabase)
            _uiState.update { it.copy(exportResult = res) }
        }
    }

    fun clearExportResult() {
        _uiState.update { it.copy(exportResult = null) }
    }
}
