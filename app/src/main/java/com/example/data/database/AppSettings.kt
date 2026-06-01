package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 0,
    val hapticEnabled: Boolean = true,
    val audioEnabled: Boolean = true,
    val advancedPrecision: Boolean = false,
    val calibrationX: Float = 0f,
    val calibrationY: Float = 0f,
    val calibrationZ: Float = 0f,
    val tolerancePerfect: Float = 0.2f, // degrees
    val toleranceNear: Float = 1.0f,    // degrees
    val sensorSmoothing: Float = 0.15f,  // Low-pass filter alpha weight (lower = smoother)
    val lastCalibratedTime: Long = 0L
)
