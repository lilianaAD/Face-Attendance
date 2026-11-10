package com.example.faceattendance.presentation.teacher.attendance

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
import com.example.faceattendance.data.local.db.entity.AttendanceMethod
import com.example.faceattendance.data.repository.AttendanceStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualAttendanceScreen(
    sessionId: Long,
    classId: Long,
    viewModel: ManualAttendanceViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val filteredRows = uiState.rows.filter { row ->
        val matchesSearch = row.student.hoTen.contains(uiState.searchQuery, ignoreCase = true) ||
                row.student.mssv.contains(uiState.searchQuery, ignoreCase = true)

        val matchesFilter = when (uiState.filter) {
            ManualFilter.ALL -> true
            ManualFilter.PRESENT -> row.status == AttendanceStatus.PRESENT || row.status == AttendanceStatus.UNCERTAIN
            ManualFilter.ABSENT -> row.status == AttendanceStatus.ABSENT
        }

        matchesSearch && matchesFilter
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Cửa 3: Điểm Danh Thủ Công", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            text = "Có mặt: ${uiState.presentCount + uiState.uncertainCount}/${uiState.totalCount} sinh viên",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Stats summary card
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Sĩ số", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                        Text("${uiState.totalCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Có mặt", fontSize = 10.sp, color = Color(0xFF16A34A))
                        Text("${uiState.presentCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Nghi vấn", fontSize = 10.sp, color = Color(0xFFD97706))
                        Text("${uiState.uncertainCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Vắng", fontSize = 10.sp, color = Color(0xFFDC2626))
                        Text("${uiState.absentCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                    }
                }
            }

            // Search input
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchChange,
                placeholder = { Text("Tìm theo tên hoặc MSSV...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Filter chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.filter == ManualFilter.ALL,
                    onClick = { viewModel.onFilterChange(ManualFilter.ALL) },
                    label = { Text("Tất cả (${uiState.totalCount})", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = uiState.filter == ManualFilter.PRESENT,
                    onClick = { viewModel.onFilterChange(ManualFilter.PRESENT) },
                    label = { Text("Có mặt (${uiState.presentCount + uiState.uncertainCount})", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = uiState.filter == ManualFilter.ABSENT,
                    onClick = { viewModel.onFilterChange(ManualFilter.ABSENT) },
                    label = { Text("Vắng (${uiState.absentCount})", fontSize = 11.sp) }
                )
            }

            // Student list
            if (uiState.isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                }
            } else if (filteredRows.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Không tìm thấy sinh viên nào.", color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredRows, key = { it.student.id }) { row ->
                        val isPresent = row.status != AttendanceStatus.ABSENT

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isPresent) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (isPresent) Icons.Default.Check else Icons.Default.Close,
                                                contentDescription = null,
                                                tint = if (isPresent) Color(0xFF15803D) else Color(0xFF94A3B8),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = row.student.hoTen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = row.student.mssv,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            row.attendance?.method?.let { method ->
                                                Text(
                                                    text = "· ${when (method) {
                                                        AttendanceMethod.FACE -> "Khuôn mặt"
                                                        AttendanceMethod.FACE_LOW_CONF -> "Cần duyệt"
                                                        AttendanceMethod.MANUAL -> "Thủ công"
                                                    }}",
                                                    fontSize = 10.sp,
                                                    color = if (method == AttendanceMethod.FACE) Color(0xFF16A34A) else Color(0xFFD97706)
                                                )
                                            }
                                        }
                                    }
                                }

                                Switch(
                                    checked = isPresent,
                                    onCheckedChange = { viewModel.toggleAttendance(row.student.id, isPresent) }
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Finish Button
            Button(
                onClick = onNavigateBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Hoàn Tất Điểm Danh", fontWeight = FontWeight.Bold)
            }
        }
    }
}
