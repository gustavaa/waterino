package se.aaro.waterino.wateringdata.usecase

import se.aaro.waterino.data.dto.SettingsDto
import se.aaro.waterino.data.ui.WaterinoSettings
import se.aaro.waterino.wateringdata.WateringDataRepository
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class UpdateSettingsUseCase @Inject constructor(
    private val wateringDataRepository: WateringDataRepository
) {

    private fun parseDate(date: String): Long =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .parse(date)
            ?.toInstant()
            ?.toEpochMilli() ?: 0

    fun WaterinoSettings.toSettingsDto() =
        SettingsDto(
            enableWatering = waterinoEnabled,
            forceNextWatering = forceNextWatering,
            lastReset = parseDate(lastDataReset),
            maxWateringTemperature = maxWateringTemperature,
            sensorReferenceValue = sensorReferenceValue,
            updateFrequencyHours = updateFrequency,
            wateringThreshold = wateringThreshold,
            wateringTimeMillis = wateringVolumeMl * 10
        )

    suspend operator fun invoke(newSettings: WaterinoSettings): Result<Unit> =
        wateringDataRepository.setWaterinoSettings(newSettings.toSettingsDto())

}
