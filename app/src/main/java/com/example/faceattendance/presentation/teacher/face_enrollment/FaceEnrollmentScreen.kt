package com.example.faceattendance.presentation.teacher.face_enrollment

import android.graphics.Bitmap
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.faceattendance.data.ml.FaceDetectorHelper
import com.example.faceattendance.data.ml.FaceNetModel
import com.example.faceattendance.data.ml.ImageQualityChecker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceEnrollmentScreen(
    studentId: Long,
    studentName: String = "",
    studentMssv: String = "",
    viewModel: FaceEnrollmentViewModel,
    onNavigateBack: () -> Unit,
    onEnrollSuccess: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(studentName, studentMssv) {
        viewModel.updateStudentInfo(studentName, studentMssv)
    }

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            onEnrollSuccess()
        }
    }

    val faceNetModel = remember { FaceNetModel(context) }
    val faceDetectorHelper = remember { FaceDetectorHelper(context) }

    DisposableEffect(Unit) {
        onDispose {
            faceNetModel.close()
            faceDetectorHelper.close()
        }
    }

    var previewView: PreviewView? by remember { mutableStateOf(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Ghi Mẫu Khuôn Mặt", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            text = if (uiState.studentName.isNotEmpty()) "${uiState.studentName} (${uiState.studentMssv})" else "MSSV: $studentId",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Đóng")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {
            // Camera Preview via CameraX
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        previewView = this

                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(surfaceProvider)
                            }

                            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview
                                )
                            } catch (exc: Exception) {
                                // Fallback to back camera if front camera not available
                                try {
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview
                                    )
                                } catch (_: Exception) {}
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Oval Overlay Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val ovalWidth = size.width * 0.72f
                val ovalHeight = ovalWidth * 1.35f
                val ovalLeft = (size.width - ovalWidth) / 2f
                val ovalTop = (size.height - ovalHeight) / 2.3f

                val ovalPath = Path().apply {
                    addOval(Rect(ovalLeft, ovalTop, ovalLeft + ovalWidth, ovalTop + ovalHeight))
                }

                // Vẽ bóng mờ xung quanh ngoài Oval
                clipPath(ovalPath, clipOp = ClipOp.Difference) {
                    drawRect(color = Color.Black.copy(alpha = 0.65f))
                }

                // Vẽ viền Oval đổi màu trạng thái
                val borderColor = when {
                    uiState.isFaceValid -> Color(0xFF22C55E) // Xanh lục: Đạt chuẩn
                    uiState.currentQualityScore > 0 -> Color(0xFFF59E0B) // Vàng: Đang chỉnh
                    else -> Color.White.copy(alpha = 0.85f)
                }

                drawPath(
                    path = ovalPath,
                    color = borderColor,
                    style = Stroke(width = 4.dp.toPx())
                )
            }

            // Top Progress Bar for 3 Steps
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        EnrollmentStep.STEP_FRONT to "1. Chính diện",
                        EnrollmentStep.STEP_LEFT to "2. Nghiêng trái",
                        EnrollmentStep.STEP_RIGHT to "3. Nghiêng phải"
                    ).forEach { (step, label) ->
                        val isDone = when (step) {
                            EnrollmentStep.STEP_FRONT -> uiState.capturedFront != null
                            EnrollmentStep.STEP_LEFT -> uiState.capturedLeft != null
                            EnrollmentStep.STEP_RIGHT -> uiState.capturedRight != null
                            else -> false
                        }
                        val isCurrent = uiState.currentStep == step

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                isDone -> Color(0xFF16A34A)
                                isCurrent -> MaterialTheme.colorScheme.primary
                                else -> Color.Black.copy(alpha = 0.5f)
                            }
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Control Area: Instruction Pill & Capture Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Instruction Pill
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.isFaceValid) Color(0xFF15803D) else Color(0xCC0F172A)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.isFaceValid) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = uiState.instructionMessage,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Shutter Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(3.dp, Color.White, CircleShape)
                    )
                    IconButton(
                        onClick = {
                            val bitmap = previewView?.bitmap ?: Bitmap.createBitmap(112, 112, Bitmap.Config.ARGB_8888)
                            val quality = ImageQualityChecker.evaluateQuality(bitmap)
                            val embedding = faceNetModel.extractEmbedding(bitmap)
                            viewModel.capturePose(bitmap, embedding, quality.qualityScore)
                        },
                        modifier = Modifier
                            .size(68.dp)
                            .background(
                                if (uiState.isFaceValid) Color(0xFF22C55E) else Color.White,
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Chụp",
                            tint = if (uiState.isFaceValid) Color.White else Color.Black,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // Review Dialog when 3 poses are captured
        if (uiState.currentStep == EnrollmentStep.STEP_REVIEW) {
            AlertDialog(
                onDismissRequest = { viewModel.resetToStep(EnrollmentStep.STEP_FRONT) },
                title = { Text("Xem Lại 3 Mẫu Khuôn Mặt", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Đã thu thập đủ 3 góc chụp chuẩn hóa 192 chiều:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "Chính diện" to uiState.capturedFront,
                                "Nghiêng trái" to uiState.capturedLeft,
                                "Nghiêng phải" to uiState.capturedRight
                            ).forEach { (label, captured) ->
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(70.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Face,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${captured?.qualityScore?.toInt() ?: 90}% đạt",
                                        fontSize = 9.sp,
                                        color = Color(0xFF16A34A)
                                    )
                                }
                            }
                        }

                        // Duplicate Warning if any
                        uiState.duplicateWarning?.let { dup ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            "Cảnh báo trùng lặp cao (${(dup.similarity * 100).toInt()}%):",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            "Khuôn mặt này trùng với SV: ${dup.hoTen} (${dup.mssv}). Vui lòng xác minh kỹ trước khi lưu.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.saveAllPoses(forceOverride = uiState.duplicateWarning != null) },
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(if (uiState.duplicateWarning != null) "Vẫn Lưu Đè" else "Lưu Mẫu")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.resetToStep(EnrollmentStep.STEP_FRONT) },
                        enabled = !uiState.isSaving
                    ) {
                        Text("Chụp Lại")
                    }
                }
            )
        }
    }
}
