package com.example.faceattendance.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.aiSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_settings_prefs")

data class AiSettings(
    val autoAcceptThreshold: Float = 0.65f,
    val needConfirmThreshold: Float = 0.55f,
    val duplicateFaceThreshold: Float = 0.75f,
    val absenceWarningThreshold: Int = 20
)

@Singleton
class AiSettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.aiSettingsDataStore

    companion object {
        val KEY_AUTO_ACCEPT = floatPreferencesKey("auto_accept_threshold")
        val KEY_NEED_CONFIRM = floatPreferencesKey("need_confirm_threshold")
        val KEY_DUPLICATE_FACE = floatPreferencesKey("duplicate_face_threshold")
        val KEY_ABSENCE_WARNING = intPreferencesKey("absence_warning_threshold")
    }

    val aiSettingsFlow: Flow<AiSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            AiSettings(
                autoAcceptThreshold = prefs[KEY_AUTO_ACCEPT] ?: 0.65f,
                needConfirmThreshold = prefs[KEY_NEED_CONFIRM] ?: 0.55f,
                duplicateFaceThreshold = prefs[KEY_DUPLICATE_FACE] ?: 0.75f,
                absenceWarningThreshold = prefs[KEY_ABSENCE_WARNING] ?: 20
            )
        }

    suspend fun updateSettings(
        autoAccept: Float,
        needConfirm: Float,
        duplicateFace: Float,
        absenceWarning: Int
    ) {
        dataStore.edit { prefs ->
            prefs[KEY_AUTO_ACCEPT] = autoAccept
            prefs[KEY_NEED_CONFIRM] = needConfirm
            prefs[KEY_DUPLICATE_FACE] = duplicateFace
            prefs[KEY_ABSENCE_WARNING] = absenceWarning
        }
    }
}
