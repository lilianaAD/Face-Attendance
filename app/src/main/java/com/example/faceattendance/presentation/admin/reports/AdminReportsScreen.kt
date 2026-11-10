package com.example.faceattendance.presentation.admin.reports

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import com.example.faceattendance.data.excel.ExportResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportsScreen(
    viewModel: AdminReportsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var exportingClassId by remember { mutableStateOf<Long?>(null) }

    val excelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        val cId = exportingClassId
        if (uri != null && cId != null) {
            viewModel.exportClassReport(cId, uri)
        }
    }

    LaunchedEffect(uiState.exportResult) {
        uiState.exportResult?.let { res ->
            when (res) {
                is ExportResult.Success -> snackbarHostState.showSnackbar(res.message)
                is ExportResult.Error -> snackbarHostState.showSnackbar("Lỗi: ${res.message}")
            }
            viewModel.clearExportResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Báo Cáo Toàn Trường", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header stats
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "TỔNG QUAN CHUYÊN CẦN TOÀN TRƯỜNG",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            letterSpacing = 1.sp
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(text = "${uiState.totalClassesCount} Lớp", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text(text = "${uiState.totalStudentsCount} Sinh viên", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${uiState.overallAttendanceRate.toInt()}%",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.overallAttendanceRate >= 80f) Color(0xFF16A34A) else Color(0xFFD97706)
                                )
                                Text(text = "Tỷ lệ trung bình", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                            }
                        }

                        if (uiState.totalDangerCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFEF2F2),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "Toàn trường có ${uiState.totalDangerCount} sinh viên có nguy cơ cấm thi (vắng >= 20%).",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF991B1B)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Class breakdown list
            item {
                Text(
                    text = "BẢNG THỐNG KÊ THEO LỚP HỌC",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.sp
                )
            }

            items(uiState.classReports) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = item.classEntity.classCode, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(text = item.classEntity.className, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                            }

                            IconButton(onClick = {
                                exportingClassId = item.classEntity.id
                                excelLauncher.launch("BaoCao_${item.classEntity.classCode}.xlsx")
                            }) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Xuất Excel", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Sĩ số: ${item.totalStudents} | Buổi học: ${item.totalSessions}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                            Text(
                                text = "Tỷ lệ: ${item.averageRate.toInt()}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (item.averageRate >= 80f) Color(0xFF16A34A) else Color(0xFFD97706)
                            )
                        }

                        if (item.dangerCount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "⚠️ ${item.dangerCount} sinh viên vắng >= 20%",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }
        }
    }
}
