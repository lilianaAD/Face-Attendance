package com.example.faceattendance.di

import com.example.faceattendance.data.repository.AuthRepository
import com.example.faceattendance.data.repository.AuthRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(
        impl: com.example.faceattendance.data.repository.SessionRepositoryImpl
    ): com.example.faceattendance.data.repository.SessionRepository

    @Binds
    @Singleton
    abstract fun bindStudentRepository(
        impl: com.example.faceattendance.data.repository.StudentRepositoryImpl
    ): com.example.faceattendance.data.repository.StudentRepository

    @Binds
    @Singleton
    abstract fun bindFaceRepository(
        impl: com.example.faceattendance.data.repository.FaceRepositoryImpl
    ): com.example.faceattendance.data.repository.FaceRepository

    @Binds
    @Singleton
    abstract fun bindAttendanceRepository(
        impl: com.example.faceattendance.data.repository.AttendanceRepositoryImpl
    ): com.example.faceattendance.data.repository.AttendanceRepository
}
