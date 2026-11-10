package com.example.faceattendance.presentation.teacher.students

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
fun StudentManagementScreen(
    viewModel: StudentViewModel,
    onNavigateBack: () -> Unit,
    onStudentClick: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val filteredStudents = uiState.students.filter { item ->
        val matchesSearch = item.student.hoTen.contains(uiState.searchQuery, ignoreCase = true) ||
                item.student.mssv.contains(uiState.searchQuery, ignoreCase = true)
        val matchesFilter = when (uiState.faceFilter) {
            FaceFilter.ALL -> true
            FaceFilter.HAS_FACE -> item.faceCount > 0
            FaceFilter.MISSING_FACE -> item.faceCount == 0
        }
        matchesSearch && matchesFilter
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản Lý Sinh Viên", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Class Tabs
            if (uiState.classes.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = uiState.classes.indexOf(uiState.selectedClass).coerceAtLeast(0),
                    edgePadding = 0.dp,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    uiState.classes.forEach { cls ->
                        Tab(
                            selected = uiState.selectedClass?.id == cls.id,
                            onClick = { viewModel.onSelectClass(cls) },
                            text = { Text(cls.classCode, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchChange,
                placeholder = { Text("Tìm theo tên hoặc MSSV...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.faceFilter == FaceFilter.ALL,
                    onClick = { viewModel.onFilterChange(FaceFilter.ALL) },
                    label = { Text("Tất cả (${uiState.students.size})", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = uiState.faceFilter == FaceFilter.HAS_FACE,
                    onClick = { viewModel.onFilterChange(FaceFilter.HAS_FACE) },
                    label = { Text("Đã có mẫu (${uiState.students.count { it.faceCount > 0 }})", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = uiState.faceFilter == FaceFilter.MISSING_FACE,
                    onClick = { viewModel.onFilterChange(FaceFilter.MISSING_FACE) },
                    label = { Text("Chưa có mẫu (${uiState.students.count { it.faceCount == 0 }})", fontSize = 11.sp) }
                )
            }

            // Student List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredStudents) { item ->
                    val s = item.student
                    Card(
                        onClick = { onStudentClick(s.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
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
                                    text = s.hoTen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${s.mssv} · ${s.email.ifEmpty { "Chưa có email" }}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (item.faceCount > 0) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                            ) {
                                Text(
                                    text = if (item.faceCount > 0) "Đã có ${item.faceCount} mẫu" else "Chưa có mẫu",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (item.faceCount > 0) Color(0xFF15803D) else Color(0xFFB45309),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
