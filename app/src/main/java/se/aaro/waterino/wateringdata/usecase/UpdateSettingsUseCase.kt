package se.aaro.waterino.wateringdata.usecase

import se.aaro.waterino.data.dto.SettingsDto
import se.aaro.waterino.data.ui.WaterinoSettings
import se.aaro.waterino.wateringdata.WateringDataRepository
import javax.inject.Inject

class UpdateSettingsUseCase @Inject constructor(
    private val wateringDataRepository: WateringDataRepository
) {

    fun WaterinoSettings.toSettingsDto() =
        SettingsDto(
            enableWatering = waterinoEnabled,
            forceNextWatering = forceNextWatering,
            lastReset = lastDataReset,
            maxWateringTemperature = maxWateringTemperature,
            sensorReferenceValue = sensorReferenceValue,
            updateFrequencyHours = updateFrequency,
            wateringThreshold = wateringThreshold,
            wateringTimeMillis = wateringVolumeMl * 10,
            wateringMode = wateringMode.name
        )

    suspend operator fun invoke(newSettings: WaterinoSettings): Result<Unit> =
        newSettings.takeUnless { it == WaterinoSettings() }
            ?.let {
                wateringDataRepository.setWaterinoSettings(it.toSettingsDto())
            } ?: Result.failure(RuntimeException("Refused to update with empty settings"))

}
