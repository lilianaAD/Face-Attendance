package com.example.faceattendance.presentation.admin.courses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.faceattendance.data.local.db.entity.CourseEntity
import com.example.faceattendance.data.local.db.entity.UserRole
import com.example.faceattendance.domain.usecase.ManageCoursesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CourseUiState(
    val courses: List<CourseEntity> = emptyList(),
    val searchQuery: String = "",
    val showCreateDialog: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class CourseViewModel @Inject constructor(
    private val manageCoursesUseCase: ManageCoursesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CourseUiState())
    val uiState: StateFlow<CourseUiState> = _uiState.asStateFlow()

    init {
        manageCoursesUseCase.getAllCourses()
            .onEach { list -> _uiState.update { it.copy(courses = list) } }
            .launchIn(viewModelScope)
    }

    fun onSearchChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleCreateDialog(show: Boolean) {
        _uiState.update { it.copy(showCreateDialog = show, errorMessage = null) }
    }

    fun createCourse(code: String, name: String, creditsStr: String) {
        val credits = creditsStr.toIntOrNull() ?: 3
        viewModelScope.launch {
            val result = manageCoursesUseCase.createCourse(UserRole.ADMIN, code, name, credits)
            result.onSuccess {
                _uiState.update { it.copy(showCreateDialog = false, successMessage = "Tạo môn học '$code' thành công!") }
            }.onFailure { ex ->
                _uiState.update { it.copy(errorMessage = ex.message) }
            }
        }
    }

    fun deleteCourse(course: CourseEntity) {
        viewModelScope.launch {
            val result = manageCoursesUseCase.deleteCourse(UserRole.ADMIN, course)
            result.onSuccess {
                _uiState.update { it.copy(successMessage = "Đã xóa môn '${course.courseCode}'") }
            }.onFailure { ex ->
                _uiState.update { it.copy(errorMessage = ex.message) }
            }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }
}
