package com.example.faceattendance.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.faceattendance.data.local.db.entity.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var autoAccept by remember(uiState.aiSettings) { mutableStateOf(uiState.aiSettings.autoAcceptThreshold) }
    var needConfirm by remember(uiState.aiSettings) { mutableStateOf(uiState.aiSettings.needConfirmThreshold) }
    var duplicateFace by remember(uiState.aiSettings) { mutableStateOf(uiState.aiSettings.duplicateFaceThreshold) }
    var absenceWarning by remember(uiState.aiSettings) { mutableStateOf(uiState.aiSettings.absenceWarningThreshold) }

    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cài Đặt Hệ Thống", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Profile Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (uiState.role == UserRole.ADMIN) Color(0xFFFEF3C7) else Color(0xFFEFF6FF),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (uiState.role == UserRole.ADMIN) Icons.Default.Shield else Icons.Default.School,
                                        contentDescription = null,
                                        tint = if (uiState.role == UserRole.ADMIN) Color(0xFFD97706) else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Column {
                                Text(text = uiState.displayName, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (uiState.role == UserRole.ADMIN) "Quản trị viên (ADMIN)" else "Giảng viên (TEACHER)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (uiState.role == UserRole.ADMIN) Color(0xFFD97706) else MaterialTheme.colorScheme.primary
                                )
                                Text(text = "@${uiState.username}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }

            // Change Password Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "ĐỔI MẬT KHẨU",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            letterSpacing = 1.sp
                        )

                        OutlinedTextField(
                            value = uiState.currentPasswordInput,
                            onValueChange = viewModel::onCurrentPasswordChanged,
                            label = { Text("Mật khẩu hiện tại") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = uiState.newPasswordInput,
                            onValueChange = viewModel::onNewPasswordChanged,
                            label = { Text("Mật khẩu mới (tối thiểu 6 ký tự)") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = uiState.confirmPasswordInput,
                            onValueChange = viewModel::onConfirmPasswordChanged,
                            label = { Text("Xác nhận mật khẩu mới") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = viewModel::changePassword,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !uiState.isLoading
                        ) {
                            Text("Cập Nhật Mật Khẩu", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // AI Thresholds (Only visible to ADMIN)
            if (uiState.role == UserRole.ADMIN) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CẤU HÌNH NGƯỠNG AI & CẢNH BÁO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    letterSpacing = 1.sp
                                )
                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFFEF3C7)) {
                                    Text(
                                        text = "ADMIN ONLY",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD97706),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Auto-accept Threshold
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "Tự động ghi nhận (Cosine):", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text(text = String.format("%.2f", autoAccept), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                                }
                                Slider(
                                    value = autoAccept,
                                    onValueChange = { autoAccept = it },
                                    valueRange = 0.50f..0.85f,
                                    steps = 7
                                )
                            }

                            // Need confirm Threshold
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "Cần xác nhận (Cosine):", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text(text = String.format("%.2f", needConfirm), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                                }
                                Slider(
                                    value = needConfirm,
                                    onValueChange = { needConfirm = it },
                                    valueRange = 0.40f..0.70f,
                                    steps = 6
                                )
                            }

                            // Duplicate face Threshold
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "Cảnh báo trùng mẫu (Cosine):", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text(text = String.format("%.2f", duplicateFace), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                }
                                Slider(
                                    value = duplicateFace,
                                    onValueChange = { duplicateFace = it },
                                    valueRange = 0.65f..0.90f,
                                    steps = 5
                                )
                            }

                            // Absence warning Threshold
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "Ngưỡng cảnh báo cấm thi:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text(text = "Vắng >= $absenceWarning%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                }
                                Slider(
                                    value = absenceWarning.toFloat(),
                                    onValueChange = { absenceWarning = it.toInt() },
                                    valueRange = 10f..40f,
                                    steps = 6
                                )
                            }

                            Button(
                                onClick = {
                                    viewModel.updateAiThresholds(autoAccept, needConfirm, duplicateFace, absenceWarning)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Lưu Cấu Hình Ngưỡng AI", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Logout Button
            item {
                OutlinedButton(
                    onClick = { viewModel.logout(onLoggedOut) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Đăng Xuất Tài Khoản", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
