package com.example.faceattendance.presentation.admin.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.faceattendance.data.backup.BackupExportResult
import com.example.faceattendance.data.backup.BackupManager
import com.example.faceattendance.data.backup.BackupRestoreStats
import com.example.faceattendance.data.local.db.AppDatabase
import com.example.faceattendance.data.local.preferences.AuthDataStore
import com.example.faceattendance.domain.permission.PermissionChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupUiState(
    val isLoading: Boolean = false,
    val lastExportResult: BackupExportResult? = null,
    val lastRestoreStats: BackupRestoreStats? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager,
    private val appDatabase: AppDatabase,
    private val authDataStore: AuthDataStore,
    private val permissionChecker: PermissionChecker
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun exportBackup(outputUri: Uri) {
        viewModelScope.launch {
            val user = authDataStore.currentUser.firstOrNull()
            try {
                permissionChecker.requireAdmin(user?.role)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val result = backupManager.exportBackup(outputUri, appDatabase)
            result.onSuccess { res ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        lastExportResult = res,
                        successMessage = "Sao lưu thành công ${res.totalRecords} bản ghi dữ liệu."
                    ) 
                }
            }.onFailure { ex ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "Lỗi sao lưu: ${ex.message}"
                    ) 
                }
            }
        }
    }

    fun restoreBackup(inputUri: Uri) {
        viewModelScope.launch {
            val user = authDataStore.currentUser.firstOrNull()
            try {
                permissionChecker.requireAdmin(user?.role)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val result = backupManager.restoreBackup(inputUri, appDatabase)
            result.onSuccess { stats ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        lastRestoreStats = stats,
                        successMessage = "Khôi phục thành công! Đã nạp lại ${stats.studentsCount} sinh viên, ${stats.classesCount} lớp, ${stats.attendancesCount} lượt điểm danh."
                    ) 
                }
            }.onFailure { ex ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "Lỗi khôi phục: ${ex.message}"
                    ) 
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
