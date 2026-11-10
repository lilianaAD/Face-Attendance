package com.example.faceattendance.presentation.admin.assignment

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
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherAssignmentScreen(
    viewModel: AssignmentViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTeacherToAssign by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Phân Công Giảng Dạy", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleAssignDialog(true) }) {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Thêm GV phụ trách")
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            uiState.successMessage?.let { msg ->
                Surface(
                    color = Color(0xFFDCFCE7),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(msg, color = Color(0xFF15803D), fontSize = 12.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = viewModel::clearFeedback) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Class Selector Chips
            Text(
                text = "1. CHỌN LỚP HỌC",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 1.sp
            )

            ScrollableTabRow(
                selectedTabIndex = uiState.classes.indexOf(uiState.selectedClass).coerceAtLeast(0),
                edgePadding = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                uiState.classes.forEach { cls ->
                    Tab(
                        selected = uiState.selectedClass?.id == cls.id,
                        onClick = { viewModel.onSelectClass(cls) },
                        text = { Text(cls.classCode, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            // Selected Class Info
            uiState.selectedClass?.let { cls ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Lớp: ${cls.className}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Mã lớp: ${cls.classCode} · ${cls.description.ifEmpty { "Không có mô tả" }}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Assigned Teachers List Header & Add button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "2. GIÁO VIÊN ĐANG PHỤ TRÁCH (${uiState.assignedTeachers.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.sp
                )

                TextButton(onClick = { viewModel.toggleAssignDialog(true) }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Gán thêm GV", fontSize = 12.sp)
                }
            }

            if (uiState.assignedTeachers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Lớp này hiện chưa có giáo viên phụ trách nào.", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.assignedTeachers) { teacher ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = teacher.displayName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "@${teacher.username} · ${teacher.email}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }

                                TextButton(
                                    onClick = { viewModel.removeAssignment(teacher.id) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFDC2626))
                                ) {
                                    Text("Gỡ", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Assign Dialog
    if (uiState.showAssignDialog) {
        val availableTeachers = uiState.teachers.filter { t ->
            !uiState.assignedTeachers.any { it.id == t.id }
        }

        AlertDialog(
            onDismissRequest = { viewModel.toggleAssignDialog(false) },
            title = { Text("Gán Giáo Viên Vào Lớp", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Chọn giáo viên để phân công vào lớp ${uiState.selectedClass?.classCode}:", fontSize = 12.sp)

                    if (availableTeachers.isEmpty()) {
                        Text("Tất cả giáo viên đã được phân công vào lớp này.", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            items(availableTeachers) { teacher ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedTeacherToAssign = teacher.id }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedTeacherToAssign == teacher.id,
                                        onClick = { selectedTeacherToAssign = teacher.id }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(teacher.displayName, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        Text("@${teacher.username}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedTeacherToAssign?.let { viewModel.assignTeacher(it) }
                        selectedTeacherToAssign = null
                    },
                    enabled = selectedTeacherToAssign != null
                ) {
                    Text("Xác nhận gán")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.toggleAssignDialog(false) }) {
                    Text("Hủy")
                }
            }
        )
    }
}
