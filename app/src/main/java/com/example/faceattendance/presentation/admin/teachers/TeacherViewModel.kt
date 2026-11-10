package com.example.faceattendance.presentation.admin.teachers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.faceattendance.data.local.db.entity.UserEntity
import com.example.faceattendance.data.local.db.entity.UserRole
import com.example.faceattendance.domain.usecase.ManageTeachersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeacherUiState(
    val teachers: List<UserEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val showCreateDialog: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class TeacherViewModel @Inject constructor(
    private val manageTeachersUseCase: ManageTeachersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherUiState())
    val uiState: StateFlow<TeacherUiState> = _uiState.asStateFlow()

    init {
        loadTeachers()
    }

    private fun loadTeachers() {
        manageTeachersUseCase.getTeachers(UserRole.ADMIN)
            .onEach { list ->
                _uiState.update { it.copy(teachers = list) }
            }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleCreateDialog(show: Boolean) {
        _uiState.update { it.copy(showCreateDialog = show, errorMessage = null) }
    }

    fun createTeacher(username: String, displayName: String, email: String) {
        viewModelScope.launch {
            val result = manageTeachersUseCase.createTeacher(UserRole.ADMIN, username, displayName, email)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        showCreateDialog = false,
                        successMessage = "Tạo giáo viên thành công! Mật khẩu mặc định: teacher123"
                    )
                }
            }.onFailure { ex ->
                _uiState.update { it.copy(errorMessage = ex.message) }
            }
        }
    }

    fun toggleStatus(teacher: UserEntity) {
        viewModelScope.launch {
            manageTeachersUseCase.toggleTeacherStatus(UserRole.ADMIN, teacher.id, !teacher.isActive)
        }
    }

    fun resetPassword(teacher: UserEntity) {
        viewModelScope.launch {
            val result = manageTeachersUseCase.resetPassword(UserRole.ADMIN, teacher.id, "teacher123")
            result.onSuccess {
                _uiState.update { it.copy(successMessage = "Đã reset mật khẩu của '${teacher.displayName}' về 'teacher123'") }
            }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }
}
