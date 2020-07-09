package se.aaro.waterino.sensordata

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import com.google.android.gms.tasks.OnCompleteListener
import java.util.prefs.Preferences


class SensorDataPresenter(val view: SensorDataContract.View, context: Context): SensorDataContract.Presenter {

    var model: SensorDataModel

    val timeUpdaterHandler = Handler()
    lateinit var timeUpdateRunnable: Runnable
    val notificationsEnabledKey = "ENABLE_NOTIFICATION"

    val preferences = context.getSharedPreferences("settings",Application.MODE_PRIVATE);


    init {
        view.setUpViews()

        model = SensorDataModel(this)
        timeUpdateRunnable = Runnable {
            view.updateTimeViews(model.lastUpdate,model.nextUpdate)
            timeUpdaterHandler.postDelayed(timeUpdateRunnable,10000)
        }
        timeUpdaterHandler.post(timeUpdateRunnable)

        val enableNotifications = preferences.getBoolean(notificationsEnabledKey,false)
        model.setNotificationsEnabled(enableNotifications,null)
        view.setNotificationsEnabled(enableNotifications)
    }

    override fun onNewSensorData(data: List<WateringData?>) {
        var wateredAmount = 0.0
        data.forEach {
            if(it!!.wateredPlant){
                wateredAmount += it.wateredAmount*0.0001
            }
        }
        view.updateWateredAmount(wateredAmount)
        view.updateLatestData(data.maxBy{ it!!.time }!!)
        view.updatePlot(data
                .sortedBy { it!!.time}
        )
        view.updateTimeViews(model.lastUpdate,model.nextUpdate)
    }

    override fun onUserEnableChange(enabled: Boolean, onCompleteListener: OnCompleteListener<Void>?) {
        model.setEnabled(enabled, onCompleteListener)
    }

    override fun onUserWateringAmountChange(amount: Int, onCompleteListener: OnCompleteListener<Void>?) {
        model.setWateringAmount(amount,onCompleteListener)
    }

    override fun onUserUpdateFrequencyChange(hours: Double, onCompleteListener: OnCompleteListener<Void>?) {
        model.setUpdateFrequency(hours,onCompleteListener)
    }

    override fun onUserMaxWateringTempChange(temperature: Int, onCompleteListener: OnCompleteListener<Void>?) {
        model.setMaxWateringTemperature(temperature,onCompleteListener)
    }

    override fun onUserForceNextChange(forceNext: Boolean, onCompleteListener: OnCompleteListener<Void>?) {
        model.setForceNext(forceNext, onCompleteListener)
    }

    override fun onUserThresholdChange(threshold: Int, onCompleteListener: OnCompleteListener<Void>?) {
        model.setThreshold(threshold, onCompleteListener)
    }

    override fun onEnabledUpdated(enabled: Boolean) {
        view.setEnabled(enabled)
    }

    override fun onForceNextUpdated(forceNext: Boolean) {
        view.setForceNext(forceNext)
    }

    override fun onThresholdUpdated(threshold: Int) {
        view.setThreshold(threshold)
    }

    override fun onWateringAmountChange(amount: Int) {
        view.setWateringAmount(amount)
    }

    override fun onUpdateFrequencyChange(hours: Double) {
        view.setUpdateFrequency(hours)
    }

    override fun onMaxTemperatureChange(maxTemperature: Int) {
        view.setMaxWateringTemperature(maxTemperature)
    }

    override fun onUserChangeNotificationsEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(notificationsEnabledKey,enabled).apply()
        model.setNotificationsEnabled(enabled, null)
    }


}