package com.example.faceattendance.presentation.admin.teachers

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
import com.example.faceattendance.data.local.db.entity.UserEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherManagementScreen(
    viewModel: TeacherViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var newUsername by remember { mutableStateOf("") }
    var newDisplayName by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }

    val filteredTeachers = uiState.teachers.filter {
        it.displayName.contains(uiState.searchQuery, ignoreCase = true) ||
                it.username.contains(uiState.searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản Lý Giáo Viên", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleCreateDialog(true) }) {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Thêm giáo viên")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.toggleCreateDialog(true) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm giáo viên", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Success or error banners
            uiState.successMessage?.let { msg ->
                Surface(
                    color = Color(0xFFDCFCE7),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(msg, color = Color(0xFF15803D), fontSize = 12.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = viewModel::clearFeedback) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                placeholder = { Text("Tìm theo tên hoặc username...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Text(
                text = "TỔNG SỐ: ${filteredTeachers.size} GIÁO VIÊN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 1.sp
            )

            // Teachers List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTeachers) { teacher ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = teacher.displayName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "@${teacher.username} · ${teacher.email.ifEmpty { "Chưa có email" }}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (teacher.isActive) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                                ) {
                                    Text(
                                        text = if (teacher.isActive) "Hoạt động" else "Đã khóa",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (teacher.isActive) Color(0xFF15803D) else Color(0xFFB91C1C),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { viewModel.resetPassword(teacher) }) {
                                    Icon(Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset mật khẩu", fontSize = 11.sp)
                                }

                                TextButton(
                                    onClick = { viewModel.toggleStatus(teacher) },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = if (teacher.isActive) Color(0xFFDC2626) else Color(0xFF16A34A)
                                    )
                                ) {
                                    Icon(
                                        if (teacher.isActive) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (teacher.isActive) "Khóa" else "Kích hoạt", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Teacher Dialog
    if (uiState.showCreateDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.toggleCreateDialog(false) },
            title = { Text("Thêm Tài Khoản Giáo Viên", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    uiState.errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                    OutlinedTextField(
                        value = newUsername,
                        onValueChange = { newUsername = it },
                        label = { Text("Tên đăng nhập (username)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newDisplayName,
                        onValueChange = { newDisplayName = it },
                        label = { Text("Họ và tên (VD: ThS. Nguyễn Văn A)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text("Email liên hệ") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "* Mật khẩu khởi tạo mặc định: teacher123",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createTeacher(newUsername, newDisplayName, newEmail)
                        newUsername = ""
                        newDisplayName = ""
                        newEmail = ""
                    }
                ) {
                    Text("Tạo mới")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.toggleCreateDialog(false) }) {
                    Text("Hủy")
                }
            }
        )
    }
}
