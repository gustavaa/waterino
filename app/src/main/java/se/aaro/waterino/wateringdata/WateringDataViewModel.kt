package se.aaro.waterino.wateringdata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import se.aaro.waterino.data.ui.CurrentPlantState
import se.aaro.waterino.data.ui.WateringData
import se.aaro.waterino.data.ui.WateringMode
import se.aaro.waterino.data.ui.WaterinoSettings
import se.aaro.waterino.wateringdata.WateringDataViewModel.UiAction.SetPushNotificationsEnabled
import se.aaro.waterino.wateringdata.usecase.GetCurrentPlantStateUseCase
import se.aaro.waterino.wateringdata.usecase.GetSettingsUseCase
import se.aaro.waterino.wateringdata.usecase.GetWateringDataUseCase
import se.aaro.waterino.wateringdata.usecase.ResetDataUseCase
import se.aaro.waterino.wateringdata.usecase.SetPushNotificationEnabledUseCase
import se.aaro.waterino.wateringdata.usecase.UpdateSettingsUseCase
import java.text.SimpleDateFormat
import java.util.Locale
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
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun formatResetString(timestamp: Long) = SimpleDateFormat(
        "yyyy-MM-dd",
        Locale.getDefault()
    ).format(timestamp)

    sealed class UiAction {
        data class SetPushNotificationsEnabled(val enabled: Boolean) : UiAction()
        data class SetWaterinoEnabled(val enabled: Boolean) : UiAction()
        data class SetForceNextWateringEnabled(val forceNext: Boolean) : UiAction()
        data class SetWateringThreshold(val threshold: Int) : UiAction()
        data class SetUpdateFrequencyHours(val updateFrequencyHours: Double) : UiAction()
        data class SetWateringVolumeMl(val wateringVolumeMl: Int) : UiAction()
        data class SetMaximumWateringTemperature(val maximumWateringTemperature: Int) : UiAction()
        data class SetWateringMode(val wateringMode: WateringMode) : UiAction()
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
            is SetPushNotificationsEnabled -> {
                setPushNotificationEnabledUseCase(uiAction.enabled).also {
                    if (it.isSuccess) {
                        _uiState.value = mergeUiStates(
                            settingsState = uiState.value.settingsState.copy(
                                pushNotificationsEnabled = uiAction.enabled
                            )
                        )
                    }
                }
            }
            UiAction.ResetData -> resetDataUseCase()
            is UiAction.SetForceNextWateringEnabled -> updateSettingsUseCase(
                uiState.value.settingsState.copy(
                    forceNextWatering = uiAction.forceNext
                )
            )
            is UiAction.SetMaximumWateringTemperature -> updateSettingsUseCase(
                uiState.value.settingsState.copy(
                    maxWateringTemperature = uiAction.maximumWateringTemperature
                )
            )
            is UiAction.SetUpdateFrequencyHours -> updateSettingsUseCase(
                uiState.value.settingsState.copy(
                    updateFrequency = uiAction.updateFrequencyHours
                )
            )
            is UiAction.SetWateringVolumeMl -> updateSettingsUseCase(
                uiState.value.settingsState.copy(
                    wateringVolumeMl = uiAction.wateringVolumeMl
                )
            )
            is UiAction.SetWateringThreshold -> updateSettingsUseCase(
                uiState.value.settingsState.copy(
                    wateringThreshold = uiAction.threshold
                )
            )
            is UiAction.SetWaterinoEnabled -> updateSettingsUseCase(
                uiState.value.settingsState.copy(
                    waterinoEnabled = uiAction.enabled
                )
            )

            is UiAction.SetWateringMode -> updateSettingsUseCase(
                uiState.value.settingsState.copy(
                    wateringMode = uiAction.wateringMode
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
}
