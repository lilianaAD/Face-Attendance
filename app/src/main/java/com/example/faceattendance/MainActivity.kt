package com.example.faceattendance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.faceattendance.presentation.common.navigation.AppNavigation
import com.example.faceattendance.presentation.theme.FaceAttendanceTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FaceAttendanceTheme {
                AppNavigation()
            }
        }
    }
}
