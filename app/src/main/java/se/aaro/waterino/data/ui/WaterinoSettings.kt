package se.aaro.waterino.data.ui

enum class WateringMode {
    AUTOMATIC,
    FIXED_FREQUENCY
}

data class WaterinoSettings(
    val pushNotificationsEnabled: Boolean = false,
    val waterinoEnabled: Boolean = false,
    val forceNextWatering: Boolean = false,
    val lastDataReset: Long = 0L,
    val wateringThreshold: Int = 0,
    val updateFrequency: Double = 0.0,
    val fixedWateringFrequencyHours: Double = 0.0,
    val wateringVolumeMl: Int = 0,
    val maxWateringTemperature: Int = 0,
    val sensorReferenceValue: Int = 0,
    val wateringMode: WateringMode = WateringMode.AUTOMATIC
)
