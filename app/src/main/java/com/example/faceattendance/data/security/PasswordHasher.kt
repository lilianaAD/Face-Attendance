package com.example.faceattendance.data.security

import at.favre.lib.crypto.bcrypt.BCrypt
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasswordHasher @Inject constructor() {

    fun hashPassword(password: String): String {
        return BCrypt.withDefaults().hashToString(10, password.toCharArray())
    }

    fun verifyPassword(password: String, passwordHash: String): Boolean {
        val result = BCrypt.verifyer().verify(password.toCharArray(), passwordHash.toCharArray())
        return result.verified
    }
}
