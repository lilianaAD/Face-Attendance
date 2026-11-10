package com.example.faceattendance.presentation.admin.classes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.faceattendance.data.local.db.entity.ClassEntity
import com.example.faceattendance.data.local.db.entity.UserRole
import com.example.faceattendance.domain.usecase.ManageClassesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClassUiState(
    val classes: List<ClassEntity> = emptyList(),
    val searchQuery: String = "",
    val showCreateDialog: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ClassViewModel @Inject constructor(
    private val manageClassesUseCase: ManageClassesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClassUiState())
    val uiState: StateFlow<ClassUiState> = _uiState.asStateFlow()

    init {
        manageClassesUseCase.getAllClasses()
            .onEach { list -> _uiState.update { it.copy(classes = list) } }
            .launchIn(viewModelScope)
    }

    fun onSearchChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleCreateDialog(show: Boolean) {
        _uiState.update { it.copy(showCreateDialog = show, errorMessage = null) }
    }

    fun createClass(code: String, name: String, desc: String) {
        viewModelScope.launch {
            val result = manageClassesUseCase.createClass(UserRole.ADMIN, code, name, desc)
            result.onSuccess {
                _uiState.update { it.copy(showCreateDialog = false, successMessage = "Tạo lớp học '$code' thành công!") }
            }.onFailure { ex ->
                _uiState.update { it.copy(errorMessage = ex.message) }
            }
        }
    }

    fun deleteClass(classEntity: ClassEntity) {
        viewModelScope.launch {
            val result = manageClassesUseCase.deleteClass(UserRole.ADMIN, classEntity)
            result.onSuccess {
                _uiState.update { it.copy(successMessage = "Đã xóa lớp '${classEntity.classCode}'") }
            }.onFailure { ex ->
                _uiState.update { it.copy(errorMessage = ex.message) }
            }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }
}
