package se.aaro.waterino.wateringdata.usecase

import se.aaro.waterino.wateringdata.SharedPreferencesRepository
import javax.inject.Inject

class SetPushNotificationEnabledUseCase @Inject constructor(
    private val sharedPreferencesRepository: SharedPreferencesRepository,
) {

    operator fun invoke(enabled: Boolean): Result<Unit> {
        sharedPreferencesRepository.pushNotificationsEnabled = enabled
        return Result.success(Unit)
    }
}
