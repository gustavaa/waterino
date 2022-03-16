package se.aaro.waterino.wateringdata

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import se.aaro.waterino.data.dto.SettingsDto
import se.aaro.waterino.data.dto.WateringDataDto

class WateringDataRepository(
    private val wateringDataRemoteDataSource: WateringDataRemoteDataSource
) {

    val waterinoSettings: Flow<SettingsDto> = wateringDataRemoteDataSource.wateringSettings
        .filter { it.isSuccess }
        .map { it.getOrThrow() }


    val wateringData: Flow<List<WateringDataDto>> = wateringDataRemoteDataSource.wateringData
        .filter { it.isSuccess }
        .map { it.getOrThrow() }

    suspend fun setWaterinoSettings(settingsDto: SettingsDto) =
        wateringDataRemoteDataSource.setWateringSettings(settingsDto)


    suspend fun resetData() =
        wateringDataRemoteDataSource.resetData()


}
