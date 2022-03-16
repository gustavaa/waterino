package se.aaro.waterino.wateringdata.usecase

import se.aaro.waterino.wateringdata.WateringDataRepository
import javax.inject.Inject

class ResetDataUseCase @Inject constructor(
    private val wateringDataRepository: WateringDataRepository
) {
    suspend operator fun invoke() =
        wateringDataRepository.resetData()

}
