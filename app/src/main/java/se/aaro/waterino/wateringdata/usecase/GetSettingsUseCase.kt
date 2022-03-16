package se.aaro.waterino.wateringdata.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import se.aaro.waterino.data.dto.SettingsDto
import se.aaro.waterino.data.ui.WaterinoSettings
import se.aaro.waterino.wateringdata.SharedPreferencesRepository
import se.aaro.waterino.wateringdata.WateringDataRepository
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class GetSettingsUseCase @Inject constructor(
    private val wateringDataRepository: WateringDataRepository,
    private val sharedPreferencesRepository: SharedPreferencesRepository
) {


    private fun formatDate(epochDate: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(epochDate))


    private fun transform(value: SettingsDto): WaterinoSettings =
        WaterinoSettings(
            pushNotificationsEnabled = sharedPreferencesRepository.pushNotificationsEnabled,
            waterinoEnabled = value.enableWatering,
            forceNextWatering = value.forceNextWatering,
            lastDataReset = formatDate(value.lastReset),
            wateringThreshold = value.wateringThreshold,
            updateFrequency = value.updateFrequencyHours,
            wateringVolumeMl = value.wateringTimeMillis / 10,
            maxWateringTemperature = value.maxWateringTemperature,
            sensorReferenceValue = value.sensorReferenceValue
        )

    operator fun invoke(): Flow<WaterinoSettings> =
        wateringDataRepository
            .waterinoSettings
            .map(::transform)
}
