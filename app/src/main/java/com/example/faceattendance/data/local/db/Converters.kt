package com.example.faceattendance.data.local.db

import androidx.room.TypeConverter
import com.example.faceattendance.data.local.db.entity.AttendanceMethod
import com.example.faceattendance.data.local.db.entity.SessionStatus
import com.example.faceattendance.data.local.db.entity.UserRole
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Converters {

    @TypeConverter
    fun fromFloatArray(array: FloatArray?): ByteArray? {
        if (array == null) return null
        val byteBuffer = ByteBuffer.allocate(array.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        for (value in array) {
            byteBuffer.putFloat(value)
        }
        return byteBuffer.array()
    }

    @TypeConverter
    fun toFloatArray(bytes: ByteArray?): FloatArray? {
        if (bytes == null || bytes.isEmpty()) return null
        val byteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val floatArray = FloatArray(bytes.size / 4)
        for (i in floatArray.indices) {
            floatArray[i] = byteBuffer.float
        }
        return floatArray
    }

    @TypeConverter
    fun fromUserRole(role: UserRole): String = role.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = UserRole.valueOf(value)

    @TypeConverter
    fun fromSessionStatus(status: SessionStatus): String = status.name

    @TypeConverter
    fun toSessionStatus(value: String): SessionStatus = SessionStatus.valueOf(value)

    @TypeConverter
    fun fromAttendanceMethod(method: AttendanceMethod): String = method.name

    @TypeConverter
    fun toAttendanceMethod(value: String): AttendanceMethod = AttendanceMethod.valueOf(value)
}
