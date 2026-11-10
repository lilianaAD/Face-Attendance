package com.example.faceattendance.presentation.admin.import_data

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.faceattendance.data.excel.ImportResult
import com.example.faceattendance.data.local.db.entity.UserRole
import com.example.faceattendance.domain.usecase.ImportDataUseCase
import com.example.faceattendance.domain.usecase.ImportType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ImportUiState(
    val selectedType: ImportType = ImportType.CLASSES_AND_STUDENTS,
    val selectedUri: Uri? = null,
    val fileName: String? = null,
    val isLoading: Boolean = false,
    val result: ImportResult? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val importDataUseCase: ImportDataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    fun onSelectType(type: ImportType) {
        _uiState.update { it.copy(selectedType = type, result = null, errorMessage = null) }
    }

    fun onFileSelected(uri: Uri, name: String) {
        _uiState.update { it.copy(selectedUri = uri, fileName = name, result = null, errorMessage = null) }
    }

    fun executeImport() {
        val state = _uiState.value
        val uri = state.selectedUri ?: return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = importDataUseCase(UserRole.ADMIN, state.selectedType, uri)
            result.onSuccess { res ->
                _uiState.update { it.copy(isLoading = false, result = res, errorMessage = null) }
            }.onFailure { ex ->
                _uiState.update { it.copy(isLoading = false, errorMessage = ex.message) }
            }
        }
    }

    fun clearResult() {
        _uiState.update { it.copy(result = null, errorMessage = null, selectedUri = null, fileName = null) }
    }
}
