package se.aaro.waterino.sensordata

import com.google.android.gms.tasks.OnCompleteListener

interface SensorDataContract {

    interface Model {

        fun setEnabled(enabled: Boolean, onCompleteListener: OnCompleteListener<Void>?)

        fun setForceNext(forceNext: Boolean, onCompleteListener: OnCompleteListener<Void>?)

        fun setThreshold(threshold: Int, onCompleteListener: OnCompleteListener<Void>?)

        fun setWateringAmount(amount: Int, onCompleteListener: OnCompleteListener<Void>?)

        fun setMaxWateringTemperature(maxTemperature: Int, onCompleteListener: OnCompleteListener<Void>?)

        fun setUpdateFrequency(hours: Double, onCompleteListener: OnCompleteListener<Void>?)

        fun setNotificationsEnabled(enabled: Boolean, onCompleteListener: OnCompleteListener<Void>?)

    }

    interface View {

        fun updatePlot(sensorData: List<WateringData>)

        fun updateLatestData(sensorData: WateringData)

        fun setEnabled(enabled: Boolean)

        fun setForceNext(forceNext: Boolean)

        fun setThreshold(threshold: Int)

        fun setWateringAmount(amount: Int)

        fun setUpdateFrequency(hours: Double)

        fun setMaxWateringTemperature(maxTemperature: Int)

        fun setUpViews()

        fun updateTimeViews(lastUpdate: Long, nextUpdate: Long)

        fun updateWateredAmount(amount: Double)

        fun setNotificationsEnabled(enabled: Boolean)

    }

    interface Presenter {

        fun onNewSensorData(data: List<WateringData>)

        fun setupNotifications()

        fun onUserForceNextChange(forceNext: Boolean, onCompleteListener: OnCompleteListener<Void>?)

        fun onUserThresholdChange(threshold: Int, onCompleteListener: OnCompleteListener<Void>?)

        fun onUserEnableChange(enabled: Boolean, onCompleteListener: OnCompleteListener<Void>?)

        fun onUserWateringAmountChange(amount: Int, onCompleteListener: OnCompleteListener<Void>?)

        fun onUserUpdateFrequencyChange(hours: Double, onCompleteListener: OnCompleteListener<Void>?)

        fun onUserMaxWateringTempChange(temperature: Int, onCompleteListener: OnCompleteListener<Void>?)

        fun onEnabledUpdated(enabled: Boolean)

        fun onForceNextUpdated(forceNext: Boolean)

        fun onThresholdUpdated(threshold: Int)

        fun onWateringAmountChange(amount: Int)

        fun onUpdateFrequencyChange(hours: Double)

        fun onMaxTemperatureChange(maxTemperature: Int)

        fun onUserChangeNotificationsEnabled(enabled: Boolean)

    }
}