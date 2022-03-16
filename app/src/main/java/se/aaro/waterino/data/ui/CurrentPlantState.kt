package se.aaro.waterino.data.ui

data class CurrentPlantState(
    val lastUpdated: String = "",
    val soilMoisture: String = "",
    val temperature: String = "",
    val humidity: String = "",
    val gaveWater: String = "",
    val totalWateredAmount: String = "",
    val nextUpdate: String = ""
)
