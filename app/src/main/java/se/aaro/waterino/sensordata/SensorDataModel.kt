package se.aaro.waterino.sensordata

import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import kotlin.math.roundToInt

class SensorDataModel(val presenter: SensorDataPresenter) : SensorDataContract.Model {

    var lastUpdate: Long = 0
    var nextUpdate: Long = 0
    var lastReset: Long = 0
    private var latestWateringData: List<WateringData> = emptyList()

    private val database = Firebase.database

    private val enableRef = database.getReference("enableWatering")
    private val forceNextRef = database.getReference("forceNextWatering")
    private val lastResetRef = database.getReference("lastReset")

    private val thresholdRef = database.getReference("wateringThreshold")
    private val wateringDataRef = database.getReference("wateringdata")

    private val wateringAmountRef = database.getReference("wateringTimeMillis")
    private val maxWateringTemperatureRef = database.getReference("maxWateringTemperature")
    private val updateFrequencyRef = database.getReference("updateFrequencyHours")

    init {

        enableRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                presenter.onEnabledUpdated(dataSnapshot.getValue<Boolean>() as Boolean)
            }

            override fun onCancelled(dataSnapshot: DatabaseError) {

            }
        })

        forceNextRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                presenter.onForceNextUpdated(dataSnapshot.getValue<Boolean>() as Boolean)
            }

            override fun onCancelled(dataSnapshot: DatabaseError) {

            }
        })

        thresholdRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                presenter.onThresholdUpdated(dataSnapshot.getValue<Int>() as Int)
            }

            override fun onCancelled(dataSnapshot: DatabaseError) {

            }
        })

        wateringDataRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val latestData =
                    dataSnapshot.children.last().getValue<WateringData>() as WateringData
                lastUpdate = latestData.time
                nextUpdate = latestData.time + latestData.nextUpdate
                latestWateringData =
                    dataSnapshot.children.mapNotNull { it.getValue<WateringData>() }
                updateWateringData()
            }

            override fun onCancelled(dataSnapshot: DatabaseError) {

            }
        })

        wateringAmountRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                presenter.onWateringAmountChange(((dataSnapshot.getValue<Int>() as Int) * 0.1).roundToInt())
            }

            override fun onCancelled(p0: DatabaseError) {

            }
        })

        maxWateringTemperatureRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                presenter.onMaxTemperatureChange(dataSnapshot.getValue<Int>() as Int)
            }

            override fun onCancelled(p0: DatabaseError) {

            }
        })

        updateFrequencyRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                presenter.onUpdateFrequencyChange(dataSnapshot.getValue<Double>() as Double)
            }

            override fun onCancelled(p0: DatabaseError) {

            }
        })

        lastResetRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                lastReset = dataSnapshot.getValue<Long?>() ?: 0
                presenter.onLastDataResetChange(lastReset)
                updateWateringData()
            }

            override fun onCancelled(p0: DatabaseError) {

            }
        })
    }

    private fun updateWateringData() {
        Log.d(
            "model",
            "updateWateringData: ${latestWateringData.filter { it.time >= lastReset }.size}"
        )
        presenter.onNewSensorData(latestWateringData.filter { it.time >= lastReset })
    }

    override fun setEnabled(enabled: Boolean, onCompleteListener: OnCompleteListener<Void>?) {
        if (onCompleteListener == null) {
            enableRef.setValue(enabled)
        } else {
            enableRef.setValue(enabled).addOnCompleteListener(onCompleteListener)
        }
    }

    override fun setForceNext(forceNext: Boolean, onCompleteListener: OnCompleteListener<Void>?) {
        if (onCompleteListener == null) {
            forceNextRef.setValue(forceNext)
        } else {
            forceNextRef.setValue(forceNext).addOnCompleteListener(onCompleteListener)
        }
    }

    override fun setThreshold(threshold: Int, onCompleteListener: OnCompleteListener<Void>?) {
        if (onCompleteListener == null) {
            thresholdRef.setValue(threshold)
        } else {
            thresholdRef.setValue(threshold).addOnCompleteListener(onCompleteListener)
        }
    }

    override fun setWateringAmount(amount: Int, onCompleteListener: OnCompleteListener<Void>?) {
        if (onCompleteListener == null) {
            wateringAmountRef.setValue(amount * 10)
        } else {
            wateringAmountRef.setValue(amount * 10).addOnCompleteListener(onCompleteListener)
        }
    }

    override fun setMaxWateringTemperature(
        maxTemperature: Int,
        onCompleteListener: OnCompleteListener<Void>?
    ) {
        if (onCompleteListener == null) {
            maxWateringTemperatureRef.setValue(maxTemperature)
        } else {
            maxWateringTemperatureRef.setValue(maxTemperature)
                .addOnCompleteListener(onCompleteListener)
        }
    }

    override fun setUpdateFrequency(hours: Double, onCompleteListener: OnCompleteListener<Void>?) {
        if (onCompleteListener == null) {
            updateFrequencyRef.setValue(hours)
        } else {
            updateFrequencyRef.setValue(hours).addOnCompleteListener(onCompleteListener)
        }
    }

    override fun setNotificationsEnabled(
        enabled: Boolean,
        onCompleteListener: OnCompleteListener<Void>?
    ) {
        if (enabled) {
            FirebaseMessaging.getInstance().subscribeToTopic("plantData")
                .addOnCompleteListener { task ->
                    when (task.isSuccessful) {
                        true -> Log.d("SensorData", "Subscribed to plantData topic successfully")
                        else -> Log.d("SensorData", "Failed to subscribe to plantData")
                    }
                }
        } else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("plantData")
                .addOnCompleteListener { task ->
                    when (task.isSuccessful) {
                        true -> Log.d("SensorData", "Unsubscribed from plantData topic ")
                        else -> Log.d("SensorData", "Failed to subscribe to plantData")
                    }
                }
        }

    }

    override fun resetData() {
        lastResetRef.setValue(System.currentTimeMillis())
    }

}

data class WateringData(val humidity: Int, val moisture: Int, val temperature:Float, val wateredPlant: Boolean, val time: Long, val moistureRaw: Int, val nextUpdate: Int, val wateredAmount: Int){
    constructor() : this(0,0,0.0f,false,0, 0, 0, 0)
}
