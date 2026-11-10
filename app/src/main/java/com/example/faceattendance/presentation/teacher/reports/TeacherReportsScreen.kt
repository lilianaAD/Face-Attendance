package com.example.faceattendance.presentation.teacher.reports

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
fun TeacherReportsScreen(
    viewModel: TeacherReportsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val excelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        uri?.let { viewModel.exportClassReport(it) }
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
                title = { Text("Báo Cáo Điểm Danh", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val selectedClass = uiState.classes.find { it.id == uiState.selectedClassId }
                            val code = selectedClass?.classCode ?: "Lop"
                            val filename = "BaoCao_${code}_Thang_${uiState.selectedMonth}.xlsx"
                            excelLauncher.launch(filename)
                        },
                        enabled = uiState.selectedClassId != null
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Xuất Excel", tint = MaterialTheme.colorScheme.primary)
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
            // Class Filter Chips
            item {
                Text(
                    text = "LỚP GIẢNG DẠY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.classes) { cl ->
                        FilterChip(
                            selected = cl.id == uiState.selectedClassId,
                            onClick = { viewModel.selectClass(cl.id) },
                            label = { Text(cl.classCode, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Month Filter
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "THÁNG BÁO CÁO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        letterSpacing = 1.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Tháng ${uiState.selectedMonth}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // 3-Card Summary Stats
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Buổi học
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(text = "Buổi học", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                            Text(
                                text = "${uiState.totalSessionsCount}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    // Tỷ lệ TB
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(text = "Tỷ lệ TB", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                            Text(
                                text = "${uiState.averageRate.toInt()}%",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.averageRate >= 80f) Color(0xFF16A34A) else Color(0xFFD97706),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    // Nguy cơ cấm thi
                    Card(
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (uiState.dangerCount > 0) Color(0xFFFEF2F2) else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Nguy cơ cấm thi",
                                fontSize = 11.sp,
                                color = if (uiState.dangerCount > 0) Color(0xFF991B1B) else MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "${uiState.dangerCount} SV",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.dangerCount > 0) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            // Student Roster with attendance breakdown
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CHI TIẾT CHUYÊN CẦN (${uiState.studentStats.size} SINH VIÊN)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        letterSpacing = 1.sp
                    )
                }
            }

            items(uiState.studentStats) { sv ->
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
                                Text(text = sv.hoTen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(text = sv.mssv, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (sv.isDanger) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFFEE2E2)
                                    ) {
                                        Text(
                                            text = "CẢNH BÁO CẤM THI",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFDC2626),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "${sv.attendanceRate.toInt()}%",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (sv.attendanceRate >= 80f) Color(0xFF16A34A) else if (sv.attendanceRate >= 60f) Color(0xFFD97706) else Color(0xFFDC2626)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { sv.attendanceRate / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = if (sv.attendanceRate >= 80f) Color(0xFF16A34A) else if (sv.attendanceRate >= 60f) Color(0xFFD97706) else Color(0xFFDC2626),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Có mặt: ${sv.attendedSessions} buổi", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                            Text(text = "Vắng: ${sv.absentSessions} buổi", fontSize = 10.sp, color = if (sv.isDanger) Color(0xFFDC2626) else MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }
}
