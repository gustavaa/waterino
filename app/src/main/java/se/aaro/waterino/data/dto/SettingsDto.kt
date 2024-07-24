package se.aaro.waterino.data.dto

import se.aaro.waterino.data.ui.WateringMode

data class SettingsDto(
    val enableWatering: Boolean = false,
    val forceNextWatering: Boolean = false,
    val lastReset: Long = 0,
    val maxWateringTemperature: Int = 0,
    val sensorReferenceValue: Int = 0,
    val updateFrequencyHours: Double = 0.0,
    val wateringThreshold: Int = 0,
    val wateringTimeMillis: Int = 0,
    val wateringMode: String = WateringMode.AUTOMATIC.name
)
