package se.aaro.waterino.di

import android.content.Context
import com.google.firebase.database.FirebaseDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import se.aaro.waterino.wateringdata.SharedPreferencesRepository
import se.aaro.waterino.wateringdata.WateringDataRemoteDataSource
import se.aaro.waterino.wateringdata.WateringDataRepository
import se.aaro.waterino.wateringdata.usecase.GetCurrentPlantStateUseCase
import se.aaro.waterino.wateringdata.usecase.GetSettingsUseCase
import se.aaro.waterino.wateringdata.usecase.GetWateringDataUseCase
import se.aaro.waterino.wateringdata.usecase.SetPushNotificationEnabledUseCase

@Module
@InstallIn(SingletonComponent::class)
class WaterinoModule {

    @Provides
    fun provideRemoteDataSource(): WateringDataRemoteDataSource =
        WateringDataRemoteDataSource(FirebaseDatabase.getInstance())

    @Provides
    fun provideWateringDataRepository(
        wateringDataRemoteDataSource: WateringDataRemoteDataSource
    ): WateringDataRepository =
        WateringDataRepository(wateringDataRemoteDataSource)

    @Provides
    fun provideSharedPreferenceRepository(
        @ApplicationContext context: Context
    ): SharedPreferencesRepository =
        SharedPreferencesRepository(
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        )

    @Provides
    fun provideGetSettingsUseCase(
        wateringDataRepository: WateringDataRepository,
        sharedPreferencesRepository: SharedPreferencesRepository
    ): GetSettingsUseCase = GetSettingsUseCase(wateringDataRepository, sharedPreferencesRepository)

    @Provides
    fun provideGetWateringDataUseCase(
        wateringDataRepository: WateringDataRepository,
    ): GetWateringDataUseCase = GetWateringDataUseCase(wateringDataRepository)

    @Provides
    fun provideGetCurrentPlantStateUseCase(
        wateringDataRepository: WateringDataRepository,
    ): GetCurrentPlantStateUseCase = GetCurrentPlantStateUseCase(wateringDataRepository)

    @Provides
    fun provideSetPushNotificationsEnabledUseCase(
        sharedPreferencesRepository: SharedPreferencesRepository
    ): SetPushNotificationEnabledUseCase =
        SetPushNotificationEnabledUseCase(sharedPreferencesRepository)
}
