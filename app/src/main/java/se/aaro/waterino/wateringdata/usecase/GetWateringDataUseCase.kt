package se.aaro.waterino.wateringdata.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import se.aaro.waterino.data.dto.WateringDataDto
import se.aaro.waterino.data.ui.WateringData
import se.aaro.waterino.wateringdata.WateringDataRepository
import javax.inject.Inject

class GetWateringDataUseCase @Inject constructor(
    private val wateringDataRepository: WateringDataRepository,
) {

    private fun transform(value: List<WateringDataDto>): List<WateringData> =
        value.map {
            WateringData(
                time = it.time,
                humidity = it.humidity.toFloat(),
                moisture = it.moisture.toFloat(),
                temperature = it.temperature,
                gaveWater = it.wateredPlant
            )
        }

    operator fun invoke(): Flow<List<WateringData>> =
        wateringDataRepository
            .wateringData
            .map(::transform)
}
