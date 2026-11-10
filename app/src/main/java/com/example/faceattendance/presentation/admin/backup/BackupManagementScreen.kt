package com.example.faceattendance.presentation.admin.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupManagementScreen(
    viewModel: BackupViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportBackup(it) }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
            showRestoreConfirmDialog = true
        }
    }

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
                title = { Text("Sao Lưu & Phục Hồi Dữ Liệu", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Backup Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFEFF6FF), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Column {
                            Text(text = "Tạo Bản Sao Lưu (Backup)", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Đóng gói toàn bộ 8 bảng Room DB & vector AI", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }

                    Text(
                        text = "Xuất dữ liệu hệ thống ra tệp JSON an toàn bao gồm: Tài khoản GV, Lớp học, Môn học, Sinh viên, Phân công, Buổi học, Mẫu khuôn mặt và Toàn bộ lịch sử điểm danh.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            val timeStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                            exportLauncher.launch("FaceAttendance_Backup_$timeStr.json")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !uiState.isLoading
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Tải Xuống Bản Sao Lưu (.json)", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Restore Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFEF2F2), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.SettingsBackupRestore, contentDescription = null, tint = Color(0xFFDC2626))
                            }
                        }
                        Column {
                            Text(text = "Phục Hồi Dữ Liệu (Restore)", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Khôi phục dữ liệu từ tệp sao lưu", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }

                    Text(
                        text = "Lưu ý: Quá trình khôi phục sẽ đồng bộ và cập nhật đè các bản ghi trùng ID trong cơ sở dữ liệu. Hãy đảm bảo bạn chọn đúng tệp sao lưu hợp lệ.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = { restoreLauncher.launch("application/json") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                        enabled = !uiState.isLoading
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Chọn Tệp Phục Hồi (.json)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            title = { Text("Xác nhận phục hồi dữ liệu?") },
            text = { Text("Hành động này sẽ nhập lại toàn bộ bản ghi từ tệp sao lưu vào cơ sở dữ liệu hiện tại. Bạn có chắc chắn muốn tiếp tục?") },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirmDialog = false
                        pendingRestoreUri?.let { viewModel.restoreBackup(it) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Đồng ý phục hồi")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}
