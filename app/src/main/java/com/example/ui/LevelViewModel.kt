package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.AppSettings
import com.example.data.database.SettingsRepository
import com.example.sensor.LevelData
import com.example.sensor.SensorFusionProvider
import com.example.util.FeedbackManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

enum class LevelMode {
    HORIZONTAL,
    VERTICAL,
    ANGLE_METER
}

enum class AlignmentState {
    PERFECT,
    NEAR,
    NOT_LEVEL
}

class LevelViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = SettingsRepository(database.appSettingsDao())
    private val sensorProvider = SensorFusionProvider(application)
    private val feedbackManager = FeedbackManager(application)

    // Current screen orientation / selected level mode
    private val _currentMode = MutableStateFlow(LevelMode.HORIZONTAL)
    val currentMode: StateFlow<LevelMode> = _currentMode.asStateFlow()

    // Loaded application settings from Database
    val appSettings: StateFlow<AppSettings> = repository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = AppSettings()
        )

    // Raw sensor values
    val rawSensorData: StateFlow<LevelData> = sensorProvider.levelData

    // Calibrated angles & states
    private val _calibratedPitch = MutableStateFlow(0f)
    val calibratedPitch: StateFlow<Float> = _calibratedPitch.asStateFlow()

    private val _calibratedRoll = MutableStateFlow(0f)
    val calibratedRoll: StateFlow<Float> = _calibratedRoll.asStateFlow()

    private val _calibratedYaw = MutableStateFlow(0f)
    val calibratedYaw: StateFlow<Float> = _calibratedYaw.asStateFlow()

    // Alignment state
    private val _alignmentState = MutableStateFlow(AlignmentState.NOT_LEVEL)
    val alignmentState: StateFlow<AlignmentState> = _alignmentState.asStateFlow()

    // Tracking the maximum deviation since last reset
    private val _maxDeviation = MutableStateFlow(0f)
    val maxDeviation: StateFlow<Float> = _maxDeviation.asStateFlow()

    init {
        // Monitor raw sensor readings and compute calibrated values
        viewModelScope.launch {
            rawSensorData.collect { raw ->
                val settings = appSettings.value
                val calX = settings.calibrationX
                val calY = settings.calibrationY
                val calZ = settings.calibrationZ

                // Update smoothing rate dynamically if user changes it
                sensorProvider.filterAlpha = settings.sensorSmoothing

                // Subtract calibration offset
                val pit = raw.pitch - calX
                val rol = raw.roll - calY
                val yaw = raw.yaw - calZ

                _calibratedPitch.value = pit
                _calibratedRoll.value = rol
                _calibratedYaw.value = yaw

                // Deviation measurement
                // In horizontal mode, deviation is 2D euclidean tilt
                // In vertical mode, deviation is looking at how close tilt is to perfectly vertical (pitch near 90 or roll near 90)
                // Let's compute a general deviation:
                val activeDeviation = getActiveDeviation(pit, rol, _currentMode.value)

                // Track historical maximum deviation
                _maxDeviation.value = max(_maxDeviation.value, activeDeviation)

                // Level state detection based on active mode's thresholds
                val state = when {
                    activeDeviation <= settings.tolerancePerfect -> AlignmentState.PERFECT
                    activeDeviation <= settings.toleranceNear -> AlignmentState.NEAR
                    else -> AlignmentState.NOT_LEVEL
                }
                _alignmentState.value = state

                // Trigger audio/haptics if inside perfect level zone
                if (state == AlignmentState.PERFECT) {
                    feedbackManager.triggerPerfectFeedback(
                        hapticsEnabled = settings.hapticEnabled,
                        audioEnabled = settings.audioEnabled
                    )
                }
            }
        }
    }

    private fun getActiveDeviation(pitch: Float, roll: Float, mode: LevelMode): Float {
        return when (mode) {
            LevelMode.HORIZONTAL -> {
                // Bullseye flat level uses combined tilt axes
                sqrt(pitch * pitch + roll * roll)
            }
            LevelMode.VERTICAL -> {
                // Linear 1D level (portrait orientation wall placement, e.g. Roll tilt)
                abs(roll)
            }
            LevelMode.ANGLE_METER -> {
                // Return pitch of angle deviation
                sqrt(pitch * pitch + roll * roll)
            }
        }
    }

    fun selectMode(mode: LevelMode) {
        _currentMode.value = mode
        resetMaxDeviation()
    }

    fun startListening() {
        sensorProvider.startListening()
    }

    fun stopListening() {
        sensorProvider.stopListening()
    }

    // Calibration Controls
    fun calibrateZero() {
        viewModelScope.launch {
            val raw = rawSensorData.value
            repository.setCalibration(raw.pitch, raw.roll, raw.yaw)
            resetMaxDeviation()
        }
    }

    fun resetCalibration() {
        viewModelScope.launch {
            repository.resetCalibration()
            resetMaxDeviation()
        }
    }

    fun resetMaxDeviation() {
        _maxDeviation.value = 0f
    }

    // Settings Toggle Operations
    fun toggleHaptic(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateHaptic(enabled)
        }
    }

    fun toggleAudio(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateAudio(enabled)
        }
    }

    fun togglePrecision(enabled: Boolean) {
        viewModelScope.launch {
            repository.updatePrecision(enabled)
        }
    }

    fun changeSmoothing(value: Float) {
        viewModelScope.launch {
            repository.updateSmoothing(value)
        }
    }

    fun resetAllSettings() {
        viewModelScope.launch {
            repository.resetAllSettings()
            resetMaxDeviation()
        }
    }

    override fun onCleared() {
        super.onCleared()
        sensorProvider.stopListening()
        feedbackManager.release()
    }
}
