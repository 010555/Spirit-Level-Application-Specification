package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2
import kotlin.math.sqrt

data class LevelData(
    val pitch: Float = 0f,  // Tilt forward/backward (degrees)
    val roll: Float = 0f,   // Tilt left/right (degrees)
    val yaw: Float = 0f,    // Azimuth/Rotation (degrees)
    val sensorTypeUsed: String = "None"
)

class SensorFusionProvider(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val _levelData = MutableStateFlow(LevelData())
    val levelData: StateFlow<LevelData> = _levelData.asStateFlow()

    // Active sensors list
    private var rotationVectorSensor: Sensor? = null
    private var gravitySensor: Sensor? = null
    private var gyroscopeSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null

    private var activeSensorType = "None"

    // Raw/Intermediary sensor values for fusion fallbacks
    private var gravityValues = FloatArray(3)
    private var accelValues = FloatArray(3)
    private var gyroValues = FloatArray(3)
    private var rMat = FloatArray(9)
    private var orientationVals = FloatArray(3)

    // Filtered/Smoothed values
    private var lastPitch = 0f
    private var lastRoll = 0f
    private var lastYaw = 0f

    // Low-pass filter alpha (weight of new sample, 1.0 = no filtering, closer to 0 = smoother/slower)
    var filterAlpha: Float = 0.15f

    // Gyro + Gravity fusion parameters
    private var lastTimestamp: Long = 0
    private var fusedPitch = 0f
    private var fusedRoll = 0f
    private val gyroWeight = 0.95f // weight for gyro integration in complementary filter

    init {
        detectAndConfigureSensors()
    }

    private fun detectAndConfigureSensors() {
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    fun startListening() {
        lastTimestamp = 0
        // Priority 1: Rotation Vector Sensor
        if (rotationVectorSensor != null) {
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
            activeSensorType = "Rotation Vector"
        }
        // Priority 2: Gravity + Gyroscope Fusion
        else if (gravitySensor != null && gyroscopeSensor != null) {
            sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_UI)
            activeSensorType = "Gravity + Gyro Fusion"
        }
        // Priority 3: Accelerometer Fallback
        else if (accelerometerSensor != null) {
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_UI)
            activeSensorType = "Accelerometer fallback"
        } else {
            activeSensorType = "No sensors available"
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.values == null) return

        var rawPitch = 0f
        var rawRoll = 0f
        var rawYaw = 0f

        val values = event.values

        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                if (values.size >= 3) {
                    try {
                        // Convert rotation vector to orientation angles
                        SensorManager.getRotationMatrixFromVector(rMat, values)
                        SensorManager.getOrientation(rMat, orientationVals)

                        // Convert radian results to degrees
                        rawYaw = Math.toDegrees(orientationVals[0].toDouble()).toFloat()
                        rawPitch = Math.toDegrees(orientationVals[1].toDouble()).toFloat()
                        rawRoll = Math.toDegrees(orientationVals[2].toDouble()).toFloat()
                    } catch (e: Exception) {
                        // Safe fallback if calculation fails
                    }
                }
            }

            Sensor.TYPE_GRAVITY -> {
                if (values.size >= 3) {
                    gravityValues[0] = values[0]
                    gravityValues[1] = values[1]
                    gravityValues[2] = values[2]
                    computeAnglesFromGravityOrAccel(gravityValues) { pitch, roll ->
                        rawPitch = pitch
                        rawRoll = roll
                    }
                }
            }

            Sensor.TYPE_GYROSCOPE -> {
                if (values.size >= 3 && lastTimestamp != 0L) {
                    val dT = (event.timestamp - lastTimestamp) * 1.0f / 1000000000.0f
                    gyroValues[0] = values[0] // pitch rate (rad/s)
                    gyroValues[1] = values[1] // roll rate (rad/s)
                    gyroValues[2] = values[2] // yaw rate (rad/s)

                    fusedPitch += Math.toDegrees((gyroValues[0] * dT).toDouble()).toFloat()
                    fusedRoll += Math.toDegrees((gyroValues[1] * dT).toDouble()).toFloat()

                    // Apply complementary filter with gravity
                    computeAnglesFromGravityOrAccel(gravityValues) { gravityPitch, gravityRoll ->
                        fusedPitch = gyroWeight * fusedPitch + (1 - gyroWeight) * gravityPitch
                        fusedRoll = gyroWeight * fusedRoll + (1 - gyroWeight) * gravityRoll
                    }
                    rawPitch = fusedPitch
                    rawRoll = fusedRoll
                    rawYaw += Math.toDegrees((gyroValues[2] * dT).toDouble()).toFloat()
                }
                lastTimestamp = event.timestamp
            }

            Sensor.TYPE_ACCELEROMETER -> {
                if (values.size >= 3) {
                    accelValues[0] = values[0]
                    accelValues[1] = values[1]
                    accelValues[2] = values[2]
                    computeAnglesFromGravityOrAccel(accelValues) { pitch, roll ->
                        rawPitch = pitch
                        rawRoll = roll
                    }
                }
            }
        }

        // Apply low-pass filter (exponential moving average) for stability & noise reduction
        lastPitch = lastPitch + filterAlpha * (rawPitch - lastPitch)
        lastRoll = lastRoll + filterAlpha * (rawRoll - lastRoll)
        lastYaw = lastYaw + filterAlpha * (rawYaw - lastYaw)

        // Make sure yaw stays within -180 to 180 degrees
        var normalizedYaw = lastYaw
        while (normalizedYaw > 180f) normalizedYaw -= 360f
        while (normalizedYaw < -180f) normalizedYaw += 360f

        _levelData.value = LevelData(
            pitch = lastPitch,
            roll = lastRoll,
            yaw = normalizedYaw,
            sensorTypeUsed = activeSensorType
        )
    }

    private fun computeAnglesFromGravityOrAccel(values: FloatArray, callback: (Float, Float) -> Unit) {
        val x = values[0]
        val y = values[1]
        val z = values[2]

        // Pitch is rotation around X-axis (leaning forward/backward)
        val pitch = atan2(-y, sqrt(x * x + z * z))
        // Roll is rotation around Y-axis (tilting left/right)
        val roll = atan2(x, z)

        callback(
            Math.toDegrees(pitch.toDouble()).toFloat(),
            Math.toDegrees(roll.toDouble()).toFloat()
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}
