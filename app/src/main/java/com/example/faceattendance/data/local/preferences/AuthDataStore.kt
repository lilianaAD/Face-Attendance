package com.example.faceattendance.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.faceattendance.data.local.db.entity.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

data class AuthSession(
    val userId: Long = -1L,
    val username: String = "",
    val displayName: String = "",
    val role: UserRole? = null,
    val isLoggedIn: Boolean = false
)
data class CurrentUser(
    val id: Long,
    val username: String,
    val displayName: String,
    val role: UserRole
)
@Singleton
class AuthDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val KEY_USER_ID = longPreferencesKey("user_id")
        val KEY_USERNAME = stringPreferencesKey("username")
        val KEY_DISPLAY_NAME = stringPreferencesKey("display_name")
        val KEY_ROLE = stringPreferencesKey("role")
        val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    }

    val authSessionFlow: Flow<AuthSession> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val userId = preferences[KEY_USER_ID] ?: -1L
            val username = preferences[KEY_USERNAME] ?: ""
            val displayName = preferences[KEY_DISPLAY_NAME] ?: ""
            val roleStr = preferences[KEY_ROLE]
            val role = roleStr?.let { runCatching { UserRole.valueOf(it) }.getOrNull() }
            val isLoggedIn = preferences[KEY_IS_LOGGED_IN] ?: false

            AuthSession(
                userId = userId,
                username = username,
                displayName = displayName,
                role = role,
                isLoggedIn = isLoggedIn && userId != -1L && role != null
            )
        }
    val currentUser: Flow<CurrentUser?> = authSessionFlow.map { s ->
        val r = s.role
        if (s.isLoggedIn && r != null) {
            CurrentUser(s.userId, s.username, s.displayName, r)
        } else null
    }
    suspend fun saveSession(userId: Long, username: String, displayName: String, role: UserRole) {
        dataStore.edit { preferences ->
            preferences[KEY_USER_ID] = userId
            preferences[KEY_USERNAME] = username
            preferences[KEY_DISPLAY_NAME] = displayName
            preferences[KEY_ROLE] = role.name
            preferences[KEY_IS_LOGGED_IN] = true
        }
    }

    suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
