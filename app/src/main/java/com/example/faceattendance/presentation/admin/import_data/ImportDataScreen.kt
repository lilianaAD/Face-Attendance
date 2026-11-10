package com.example.faceattendance.presentation.admin.import_data

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.faceattendance.domain.usecase.ImportType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportDataScreen(
    viewModel: ImportViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.onFileSelected(it, "file_import_${System.currentTimeMillis()}.csv")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Dữ Liệu CSV", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Segmented selection
            Text(
                text = "1. CHỌN LOẠI DỮ LIỆU CẦN IMPORT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 1.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedType == ImportType.CLASSES_AND_STUDENTS,
                    onClick = { viewModel.onSelectType(ImportType.CLASSES_AND_STUDENTS) },
                    label = { Text("Lớp & Sinh viên", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = uiState.selectedType == ImportType.COURSES,
                    onClick = { viewModel.onSelectType(ImportType.COURSES) },
                    label = { Text("Môn học", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = uiState.selectedType == ImportType.TEACHER_CLASS_ASSIGNMENTS,
                    onClick = { viewModel.onSelectType(ImportType.TEACHER_CLASS_ASSIGNMENTS) },
                    label = { Text("Gán GV-Lớp", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Instructions Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = when (uiState.selectedType) {
                            ImportType.CLASSES_AND_STUDENTS -> "Cấu trúc cột: MSSV, Ho Ten, Lop, Email"
                            ImportType.COURSES -> "Cấu trúc cột: Ma Mon, Ten Mon, So Tin Chi"
                            ImportType.TEACHER_CLASS_ASSIGNMENTS -> "Cấu trúc cột: Username GV, Ma Lop"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Dòng đầu tiên là tiêu đề (header) sẽ được tự động bỏ qua.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // File Selection Box
            Text(
                text = "2. CHỌN FILE CSV TỪ THIẾT BỊ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 1.sp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .border(
                        width = 1.5.dp,
                        color = if (uiState.selectedUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                    .clickable { filePickerLauncher.launch("text/*") }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (uiState.selectedUri != null) Icons.Default.CheckCircle else Icons.Default.FileUpload,
                        contentDescription = null,
                        tint = if (uiState.selectedUri != null) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = uiState.fileName ?: "Nhấn để chọn file CSV (*.csv)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (uiState.selectedUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Execute Import Button
            Button(
                onClick = viewModel::executeImport,
                enabled = uiState.selectedUri != null && !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Bắt đầu Import dữ liệu", fontWeight = FontWeight.SemiBold)
                }
            }

            // Result Display
            uiState.result?.let { result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "KẾT QUẢ IMPORT",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tổng dòng: ${result.totalRows}", fontSize = 12.sp)
                            Text("Đã thêm: ${result.added}", fontSize = 12.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                            Text("Bỏ qua (trùng): ${result.skipped}", fontSize = 12.sp, color = Color(0xFFD97706))
                            Text("Lỗi: ${result.errors.size}", fontSize = 12.sp, color = Color(0xFFDC2626))
                        }

                        if (result.errors.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Chi tiết lỗi:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                            LazyColumn(modifier = Modifier.heightIn(max = 120.dp)) {
                                items(result.errors) { err ->
                                    Text(
                                        text = "• Dòng ${err.rowNumber}: ${err.message}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
