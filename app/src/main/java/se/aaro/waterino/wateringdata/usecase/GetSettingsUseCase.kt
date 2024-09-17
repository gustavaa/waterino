package se.aaro.waterino.wateringdata.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import se.aaro.waterino.data.dto.SettingsDto
import se.aaro.waterino.data.ui.WateringMode
import se.aaro.waterino.data.ui.WaterinoSettings
import se.aaro.waterino.wateringdata.SharedPreferencesRepository
import se.aaro.waterino.wateringdata.WateringDataRepository
import javax.inject.Inject

class GetSettingsUseCase @Inject constructor(
    private val wateringDataRepository: WateringDataRepository,
    private val sharedPreferencesRepository: SharedPreferencesRepository
) {

    private fun transform(value: SettingsDto): WaterinoSettings =
        WaterinoSettings(
            pushNotificationsEnabled = sharedPreferencesRepository.pushNotificationsEnabled,
            waterinoEnabled = value.enableWatering,
            forceNextWatering = value.forceNextWatering,
            lastDataReset = value.lastReset,
            wateringThreshold = value.wateringThreshold,
            updateFrequency = value.updateFrequencyHours,
            fixedWateringFrequencyHours = value.fixedWateringFrequencyHours,
            wateringVolumeMl = value.wateringTimeMillis / 10,
            maxWateringTemperature = value.maxWateringTemperature,
            wateringMode = WateringMode.valueOf(value.wateringMode)
        )

    operator fun invoke(): Flow<WaterinoSettings> =
        wateringDataRepository
            .waterinoSettings
            .map(::transform)
}
