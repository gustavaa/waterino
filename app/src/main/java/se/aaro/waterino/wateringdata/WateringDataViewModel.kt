package se.aaro.waterino.wateringdata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import se.aaro.waterino.data.ui.CurrentPlantState
import se.aaro.waterino.data.ui.WateringData
import se.aaro.waterino.data.ui.WaterinoSettings
import se.aaro.waterino.wateringdata.WateringDataViewModel.UiAction.SetPushNotificationsEnabled
import se.aaro.waterino.wateringdata.usecase.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WateringDataViewModel @Inject constructor(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val getWateringDataUseCase: GetWateringDataUseCase,
    private val getCurrentPlantStateUseCase: GetCurrentPlantStateUseCase,
    private val setPushNotificationEnabledUseCase: SetPushNotificationEnabledUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase,
    private val resetDataUseCase: ResetDataUseCase

) : ViewModel() {

    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    sealed class UiAction {
        data class SetPushNotificationsEnabled(val enabled: Boolean) : UiAction()
        data class SetWaterinoEnabled(val enabled: Boolean) : UiAction()
        data class SetForceNextWateringEnabled(val forceNext: Boolean) : UiAction()
        data class SetWateringThreshold(val threshold: Int) : UiAction()
        data class SetUpdateFrequencyHours(val updateFrequencyHours: Double) : UiAction()
        data class SetWateringVolumeMl(val wateringVolumeMl: Int) : UiAction()
        data class SetMaximumWateringTemperature(val maximumWateringTemperature: Int) : UiAction()
        object ResetData : UiAction()
    }

    data class UiState(
        var settingsState: WaterinoSettings = WaterinoSettings(),
        var currentPlantState: CurrentPlantState = CurrentPlantState(),
        var wateringData: List<WateringData> = emptyList()
    )

    init {
        viewModelScope.launch {
            merge(
                getSettingsUseCase(),
                getWateringDataUseCase(),
                getCurrentPlantStateUseCase()
            ).collectLatest {
                when (it) {
                    is WaterinoSettings -> _uiState.value = mergeUiStates(settingsState = it)
                    is CurrentPlantState -> _uiState.value = mergeUiStates(currentPlantState = it)
                    is List<*> -> _uiState.value =
                        mergeUiStates(wateringData = it as List<WateringData>)
                }
            }
        }
    }

    suspend fun onUiAction(uiAction: UiAction): Result<Unit> {
        return when (uiAction) {
            is SetPushNotificationsEnabled -> setPushNotificationEnabledUseCase(uiAction.enabled)
            UiAction.ResetData -> resetDataUseCase()
            is UiAction.SetForceNextWateringEnabled -> updateSettingsUseCase(
                uiState.value.settingsState.modify(
                    forceNextWatering = uiAction.forceNext
                )
            )
            is UiAction.SetMaximumWateringTemperature -> updateSettingsUseCase(
                uiState.value.settingsState.modify(
                    maxWateringTemperature = uiAction.maximumWateringTemperature
                )
            )
            is UiAction.SetUpdateFrequencyHours -> updateSettingsUseCase(
                uiState.value.settingsState.modify(
                    updateFrequency = uiAction.updateFrequencyHours
                )
            )
            is UiAction.SetWateringVolumeMl -> updateSettingsUseCase(
                uiState.value.settingsState.modify(
                    wateringAmountMl = uiAction.wateringVolumeMl
                )
            )
            is UiAction.SetWateringThreshold -> updateSettingsUseCase(
                uiState.value.settingsState.modify(
                    wateringThreshold = uiAction.threshold
                )
            )
            is UiAction.SetWaterinoEnabled -> updateSettingsUseCase(
                uiState.value.settingsState.modify(
                    waterinoEnabled = uiAction.enabled
                )
            )
        }
    }

    private fun mergeUiStates(
        settingsState: WaterinoSettings = _uiState.value.settingsState,
        currentPlantState: CurrentPlantState = _uiState.value.currentPlantState,
        wateringData: List<WateringData> = _uiState.value.wateringData
    ): UiState = UiState(
        settingsState,
        currentPlantState,
        wateringData
    )


    fun WaterinoSettings.modify(
        pushNotificationsEnabled: Boolean = this.pushNotificationsEnabled,
        waterinoEnabled: Boolean = this.waterinoEnabled,
        forceNextWatering: Boolean = this.forceNextWatering,
        lastDataReset: String = this.lastDataReset,
        wateringThreshold: Int = this.wateringThreshold,
        updateFrequency: Double = this.updateFrequency,
        wateringAmountMl: Int = this.wateringVolumeMl,
        maxWateringTemperature: Int = this.maxWateringTemperature
    ): WaterinoSettings = WaterinoSettings(
        pushNotificationsEnabled,
        waterinoEnabled,
        forceNextWatering,
        lastDataReset,
        wateringThreshold,
        updateFrequency,
        wateringAmountMl,
        maxWateringTemperature
    )
}
