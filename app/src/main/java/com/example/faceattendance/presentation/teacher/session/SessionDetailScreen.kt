package com.example.faceattendance.presentation.teacher.session

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    viewModel: SessionDetailViewModel,
    onNavigateBack: () -> Unit,
    onOpenClassCamera: () -> Unit = {},
    onOpenSingleCamera: () -> Unit = {},
    onOpenManualAttendance: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val excelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        uri?.let { viewModel.exportSessionExcel(it) }
    }

    val filteredRows = uiState.studentRows.filter {
        it.hoTen.contains(searchQuery, ignoreCase = true) ||
                it.mssv.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.sessionDetails?.courseName ?: "Chi Tiết Buổi Học",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1
                        )
                        Text(
                            text = "${uiState.sessionDetails?.className} · ${uiState.sessionDetails?.session?.room}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::openSummaryDialog) {
                        Icon(
                            imageVector = Icons.Default.Summarize,
                            contentDescription = "Xem tổng kết",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(onClick = viewModel::toggleSessionStatus) {
                        Text(
                            text = if (uiState.isOpen) "Đóng buổi" else "Mở lại",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (uiState.isOpen) Color(0xFFDC2626) else Color(0xFF16A34A)
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Stats Hero Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TIẾN ĐỘ ĐIỂM DANH",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                letterSpacing = 1.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (uiState.isOpen) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = if (uiState.isOpen) "ĐANG MỞ" else "ĐÃ ĐÓNG",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.isOpen) Color(0xFF15803D) else Color(0xFF64748B),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${uiState.attendedCount}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "/${uiState.totalStudents} sinh viên",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "${String.format("%.1f", uiState.attendanceRate)}%",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.attendanceRate >= 80) Color(0xFF16A34A) else Color(0xFFD97706),
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { (uiState.attendanceRate / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Phòng: ${uiState.sessionDetails?.session?.room ?: "A2-301"}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Giờ: ${uiState.sessionDetails?.session?.startTime} - ${uiState.sessionDetails?.session?.endTime}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            // 3 Door Attendance Hub Header
            item {
                Text(
                    text = "3 HÌNH THỨC ĐIỂM DANH (GIAI ĐOẠN 5)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.sp
                )
            }

            // 3 Door Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        onClick = onOpenClassCamera,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Cửa 1: Cả lớp", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Chụp toàn cảnh", fontSize = 9.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }

                    Card(
                        onClick = onOpenSingleCamera,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Face, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Cửa 2: Cận mặt", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Khung oval", fontSize = 9.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }

                    Card(
                        onClick = onOpenManualAttendance,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Checklist, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Cửa 3: Thủ công", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Checklist", fontSize = 9.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }

            // Student Roster Header & Search
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DANH SÁCH LỚP (${filteredRows.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        letterSpacing = 1.sp
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Tìm theo tên hoặc MSSV...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Student Roster Rows
            items(filteredRows) { student ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = student.hoTen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.padding(top = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = student.mssv, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                                if (student.faceCount == 0) {
                                    Text(text = "· Chưa có mẫu mặt", fontSize = 10.sp, color = Color(0xFFD97706))
                                } else {
                                    Text(text = "· Đã có ${student.faceCount} mẫu", fontSize = 10.sp, color = Color(0xFF16A34A))
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (student.isAttended) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = if (student.isAttended) "Có mặt" else "Chưa thấy",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (student.isAttended) Color(0xFF15803D) else Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Session Summary Dialog
        SessionCloseSummaryDialog(
            isOpen = uiState.showSummaryDialog,
            courseName = uiState.sessionDetails?.courseName ?: "Buổi học",
            className = uiState.sessionDetails?.className ?: "",
            totalStudents = uiState.totalStudents,
            attendedCount = uiState.attendedCount,
            absentRows = uiState.studentRows.filter { !it.isAttended },
            onExportExcel = {
                val filename = "DiemDanh_${uiState.sessionDetails?.courseCode ?: "MH"}_${uiState.sessionDetails?.session?.sessionDate ?: "Ngay"}.xlsx"
                excelLauncher.launch(filename)
            },
            onDismiss = viewModel::dismissSummaryDialog
        )
    }
}
