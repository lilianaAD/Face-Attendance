package com.example.faceattendance.presentation.teacher.attendance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.faceattendance.data.repository.AttendanceStatus
import com.example.faceattendance.data.repository.StudentAttendanceRow
import com.example.faceattendance.domain.usecase.MarkManualAttendanceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ManualFilter {
    ALL,
    PRESENT,
    ABSENT
}

data class ManualAttendanceUiState(
    val sessionId: Long = 0L,
    val classId: Long = 0L,
    val rows: List<StudentAttendanceRow> = emptyList(),
    val searchQuery: String = "",
    val filter: ManualFilter = ManualFilter.ALL,
    val isLoading: Boolean = true
) {
    val totalCount: Int get() = rows.size
    val presentCount: Int get() = rows.count { it.status == AttendanceStatus.PRESENT }
    val uncertainCount: Int get() = rows.count { it.status == AttendanceStatus.UNCERTAIN }
    val absentCount: Int get() = rows.count { it.status == AttendanceStatus.ABSENT }
}

@HiltViewModel
class ManualAttendanceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val markManualUseCase: MarkManualAttendanceUseCase
) : ViewModel() {

    val sessionId: Long = savedStateHandle.get<Long>("sessionId") ?: 0L
    val classId: Long = savedStateHandle.get<Long>("classId") ?: 0L

    private val _uiState = MutableStateFlow(ManualAttendanceUiState(sessionId = sessionId, classId = classId))
    val uiState: StateFlow<ManualAttendanceUiState> = _uiState.asStateFlow()

    init {
        loadRoster()
    }

    private fun loadRoster() {
        markManualUseCase.observeRoster(sessionId, classId)
            .onEach { studentRows ->
                _uiState.update {
                    it.copy(
                        rows = studentRows,
                        isLoading = false
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onSearchChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onFilterChange(filter: ManualFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun toggleAttendance(studentId: Long, currentlyPresent: Boolean) {
        viewModelScope.launch {
            markManualUseCase.toggleAttendance(
                sessionId = sessionId,
                studentId = studentId,
                currentlyPresent = currentlyPresent,
                note = "Điểm danh thủ công (Checklist)"
            )
        }
    }
}
