package se.aaro.waterino.wateringdata.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import se.aaro.waterino.data.dto.WateringDataDto
import se.aaro.waterino.data.ui.CurrentPlantState
import se.aaro.waterino.wateringdata.WateringDataRepository
import javax.inject.Inject

class GetCurrentPlantStateUseCase @Inject constructor(
    private val wateringDataRepository: WateringDataRepository,
) {

    private fun transform(value: List<WateringDataDto>): CurrentPlantState =
        value.lastOrNull()?.let { latestWateringData ->
            CurrentPlantState(
                lastUpdated = latestWateringData.time,
                soilMoisture = "${latestWateringData.moisture}%",
                temperature = "${latestWateringData.temperature}°C",
                humidity = "${latestWateringData.humidity}%",
                gaveWater = if (latestWateringData.wateredPlant) "Yes" else "No",
                totalWateredAmount = value.sumOf { it.wateredAmount * 0.0001 }.let { "$it l" },
                nextUpdate = latestWateringData.time + latestWateringData.nextUpdate
            )
        } ?: CurrentPlantState()

    operator fun invoke(): Flow<CurrentPlantState> =
        wateringDataRepository
            .wateringData
            .map(::transform)
}
