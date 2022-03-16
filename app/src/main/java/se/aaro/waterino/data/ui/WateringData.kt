package se.aaro.waterino.data.ui

data class WateringData(
    val time: Long,
    val gaveWater: Boolean,
    val moisture: Float,
    val humidity: Float,
    val temperature: Float
)
