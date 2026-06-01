package com.example.data.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val dao: AppSettingsDao) {

    val settingsFlow: Flow<AppSettings> = dao.getSettingsFlow().map { savedSettings ->
        savedSettings ?: AppSettings()
    }

    suspend fun getSettings(): AppSettings {
        return dao.getSettings() ?: AppSettings()
    }

    suspend fun updateSettings(settings: AppSettings) {
        dao.insertSettings(settings)
    }

    suspend fun updateHaptic(enabled: Boolean) {
        val current = getSettings()
        dao.insertSettings(current.copy(hapticEnabled = enabled))
    }

    suspend fun updateAudio(enabled: Boolean) {
        val current = getSettings()
        dao.insertSettings(current.copy(audioEnabled = enabled))
    }

    suspend fun updatePrecision(enabled: Boolean) {
        val current = getSettings()
        dao.insertSettings(current.copy(advancedPrecision = enabled))
    }

    suspend fun setCalibration(x: Float, y: Float, z: Float) {
        val current = getSettings()
        dao.insertSettings(
            current.copy(
                calibrationX = x,
                calibrationY = y,
                calibrationZ = z,
                lastCalibratedTime = System.currentTimeMillis()
            )
        )
    }

    suspend fun resetCalibration() {
        val current = getSettings()
        dao.insertSettings(
            current.copy(
                calibrationX = 0f,
                calibrationY = 0f,
                calibrationZ = 0f,
                lastCalibratedTime = 0L
            )
        )
    }

    suspend fun updateSmoothing(smoothing: Float) {
        val current = getSettings()
        dao.insertSettings(current.copy(sensorSmoothing = smoothing))
    }

    suspend fun resetAllSettings() {
        dao.insertSettings(AppSettings())
    }
}
