package com.example.faceattendance.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.faceattendance.data.local.db.AppDatabase
import com.example.faceattendance.data.local.db.entity.UserRole
import com.example.faceattendance.data.local.preferences.AiSettings
import com.example.faceattendance.data.local.preferences.AiSettingsDataStore
import com.example.faceattendance.data.local.preferences.AuthDataStore
import com.example.faceattendance.data.security.PasswordHasher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val username: String = "",
    val displayName: String = "",
    val role: UserRole? = null,
    val aiSettings: AiSettings = AiSettings(),
    val currentPasswordInput: String = "",
    val newPasswordInput: String = "",
    val confirmPasswordInput: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authDataStore: AuthDataStore,
    private val aiSettingsDataStore: AiSettingsDataStore,
    private val appDatabase: AppDatabase,
    private val passwordHasher: PasswordHasher
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authDataStore.authSessionFlow.collect { session ->
                _uiState.update { 
                    it.copy(
                        username = session.username,
                        displayName = session.displayName,
                        role = session.role
                    ) 
                }
            }
        }

        viewModelScope.launch {
            aiSettingsDataStore.aiSettingsFlow.collect { aiSettings ->
                _uiState.update { it.copy(aiSettings = aiSettings) }
            }
        }
    }

    fun onCurrentPasswordChanged(value: String) = _uiState.update { it.copy(currentPasswordInput = value) }
    fun onNewPasswordChanged(value: String) = _uiState.update { it.copy(newPasswordInput = value) }
    fun onConfirmPasswordChanged(value: String) = _uiState.update { it.copy(confirmPasswordInput = value) }

    fun changePassword() {
        val state = _uiState.value
        if (state.currentPasswordInput.isBlank() || state.newPasswordInput.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Vui lòng nhập đầy đủ mật khẩu") }
            return
        }

        if (state.newPasswordInput != state.confirmPasswordInput) {
            _uiState.update { it.copy(errorMessage = "Xác nhận mật khẩu mới không khớp") }
            return
        }

        if (state.newPasswordInput.length < 6) {
            _uiState.update { it.copy(errorMessage = "Mật khẩu mới phải có ít nhất 6 ký tự") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val user = authDataStore.currentUser.firstOrNull()
            if (user == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Phiên đăng nhập hết hạn") }
                return@launch
            }

            val userEntity = appDatabase.userDao().getById(user.id)
            if (userEntity == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Không tìm thấy tài khoản") }
                return@launch
            }

            val isMatch = passwordHasher.verifyPassword(state.currentPasswordInput, userEntity.passwordHash)
            if (!isMatch) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Mật khẩu hiện tại không đúng") }
                return@launch
            }

            val newHash = passwordHasher.hashPassword(state.newPasswordInput)
            appDatabase.userDao().updatePassword(user.id, newHash)

            _uiState.update { 
                it.copy(
                    isLoading = false,
                    currentPasswordInput = "",
                    newPasswordInput = "",
                    confirmPasswordInput = "",
                    successMessage = "Đổi mật khẩu thành công!"
                ) 
            }
        }
    }

    fun updateAiThresholds(autoAccept: Float, needConfirm: Float, duplicateFace: Float, absenceWarning: Int) {
        viewModelScope.launch {
            aiSettingsDataStore.updateSettings(autoAccept, needConfirm, duplicateFace, absenceWarning)
            _uiState.update { it.copy(successMessage = "Đã lưu cấu hình ngưỡng AI thành công!") }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authDataStore.clearSession()
            onLoggedOut()
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
