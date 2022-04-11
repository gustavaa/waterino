package se.aaro.waterino.data.dto

data class WateringDataDto(
    val humidity: Int = 0,
    val moisture: Int = 0,
    val temperature: Float = 0f,
    val wateredPlant: Boolean = false,
    val time: Long = 0L,
    val moistureRaw: Int = 0,
    val nextUpdate: Long = 0,
    val wateredAmount: Int = 0
)
