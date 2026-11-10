package com.example.faceattendance.presentation.teacher.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.faceattendance.data.local.db.dao.SessionWithDetails
import com.example.faceattendance.data.local.preferences.AuthDataStore
import com.example.faceattendance.domain.usecase.GetTodaySessionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeacherHomeUiState(
    val teacherName: String = "Giáo viên",
    val teacherId: Long = 2L,
    val todaySessions: List<SessionWithDetails> = emptyList(),
    val isLoading: Boolean = false,
    val missingFaceWarning: String? = null
)

@HiltViewModel
class TeacherHomeViewModel @Inject constructor(
    private val authDataStore: AuthDataStore,
    private val getTodaySessionsUseCase: GetTodaySessionsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherHomeUiState())
    val uiState: StateFlow<TeacherHomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authDataStore.authSessionFlow.collect { session ->
                if (session.isLoggedIn && session.userId != -1L) {
                    _uiState.update { it.copy(teacherName = session.displayName, teacherId = session.userId) }
                    loadTodaySessions(session.userId)
                }
            }
        }
    }

    private fun loadTodaySessions(teacherId: Long) {
        getTodaySessionsUseCase(teacherId)
            .onEach { sessions ->
                _uiState.update {
                    it.copy(
                        todaySessions = sessions,
                        missingFaceWarning = if (sessions.isNotEmpty()) {
                            "Nhắc nhở: Lớp ${sessions.first().className} có một số sinh viên chưa có mẫu khuôn mặt."
                        } else null
                    )
                }
            }
            .launchIn(viewModelScope)
    }
}
