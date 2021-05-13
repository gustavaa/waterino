package se.aaro.waterino.sensordata

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnticipateInterpolator
import android.view.animation.AnticipateOvershootInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.anychart.charts.Cartesian
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.LimitLine.LimitLabelPosition
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.Utils
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import kotlinx.android.synthetic.main.activity_main.*
import se.aaro.waterino.R
import se.aaro.waterino.utils.collapse
import se.aaro.waterino.utils.expand
import se.aaro.waterino.utils.getTimeAgo
import se.aaro.waterino.utils.getTimeUntil
import java.lang.NumberFormatException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity(), SensorDataContract.View {

    lateinit var presenter: SensorDataPresenter
    var isSettingsExpanded = false

    companion object {
        private const val TAG = "SensorActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        presenter = SensorDataPresenter(this, this)
        presenter.setupNotifications()
    }

    override fun setUpViews() {
        enable_watering_chip.setOnCheckedChangeListener { _, isChecked ->
            presenter.onUserEnableChange(isChecked, null)
        }

        force_next.setOnCheckedChangeListener { _, isChecked ->
            presenter.onUserForceNextChange(isChecked, null)
        }

        enable_notifications_chip.setOnCheckedChangeListener { _, isChecked ->
            presenter.onUserChangeNotificationsEnabled(isChecked)
        }

        settings_card.setOnClickListener { v ->
            when(isSettingsExpanded){
                true -> settings_container.collapse(object: Animation.AnimationListener{
                    override fun onAnimationRepeat(p0: Animation?) {}
                    override fun onAnimationStart(p0: Animation?) {
                        expand_collapse_button.animate().rotation(0F).setDuration(400).setInterpolator(AnticipateOvershootInterpolator()).start()
                    }
                    override fun onAnimationEnd(p0: Animation?) {
                        isSettingsExpanded = false
                    }
                })
                false -> settings_container.expand(object: Animation.AnimationListener{
                    override fun onAnimationRepeat(p0: Animation?) {}
                    override fun onAnimationStart(p0: Animation?) {
                        expand_collapse_button.animate().rotation(180F).setDuration(400).setInterpolator(
                            AnticipateOvershootInterpolator()
                        ).start()
                    }
                    override fun onAnimationEnd(p0: Animation?) {
                        isSettingsExpanded = true
                    }
                })
            }
        }

        settings_container.collapse(null)

        set_watering_threshold.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                try {
                    set_watering_threshold.isEnabled = false
                    val newThreshold = set_watering_threshold.text.toString().toInt()
                    if (newThreshold < 0) throw IllegalArgumentException()

                    presenter.onUserThresholdChange(newThreshold, object : OnCompleteListener<Void> {
                        override fun onComplete(p0: Task<Void>) {
                            Toast.makeText(this@MainActivity, "Threshold set to $newThreshold", Toast.LENGTH_LONG)
                                .show()
                            set_watering_threshold.isEnabled = true
                        }
                    })

                } catch (e: java.lang.Exception) {
                    set_watering_threshold.isEnabled = true
                    Toast.makeText(this, "Threshold must be a signed integer", Toast.LENGTH_LONG).show()
                }
                true
            } else {
                false
            }
        }

        set_update_frequency.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                try {
                    set_update_frequency.isEnabled = false
                    val newFrequency = set_update_frequency.text.toString().toDouble()
                    if (newFrequency < 0) throw IllegalArgumentException()

                    presenter.onUserUpdateFrequencyChange(newFrequency, object : OnCompleteListener<Void> {
                        override fun onComplete(p0: Task<Void>) {
                            Toast.makeText(
                                this@MainActivity,
                                "Update frequency set to $newFrequency hours",
                                Toast.LENGTH_LONG
                            ).show()
                            set_update_frequency.isEnabled = true
                        }
                    })

                } catch (e: java.lang.Exception) {
                    set_update_frequency.isEnabled = true
                    Toast.makeText(this, "Frequency must be positive", Toast.LENGTH_LONG).show()
                }
                true
            } else {
                false
            }
        }

        set_watering_amount.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                try {
                    set_watering_amount.isEnabled = false
                    val newAmount = set_watering_amount.text.toString().toInt()
                    if (newAmount < 0) throw IllegalArgumentException()

                    presenter.onUserWateringAmountChange(newAmount, object : OnCompleteListener<Void> {
                        override fun onComplete(p0: Task<Void>) {
                            Toast.makeText(this@MainActivity, "Amount set to $newAmount", Toast.LENGTH_LONG).show()
                            set_watering_amount.isEnabled = true
                        }
                    })

                } catch (e: java.lang.Exception) {
                    set_watering_amount.isEnabled = true
                    Toast.makeText(this, "Watering amount must be a signed integer", Toast.LENGTH_LONG).show()
                }
                true
            } else {
                false
            }
        }

        set_max_temperature.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                try {
                    set_max_temperature.isEnabled = false
                    val newMaxtemp = set_max_temperature.text.toString().toInt()
                    if (newMaxtemp < 0) throw IllegalArgumentException()

                    presenter.onUserMaxWateringTempChange(newMaxtemp, object : OnCompleteListener<Void> {
                        override fun onComplete(p0: Task<Void>) {
                            Toast.makeText(
                                this@MainActivity,
                                "Maximum watering temperature set to $newMaxtemp",
                                Toast.LENGTH_LONG
                            ).show()
                            set_max_temperature.isEnabled = true
                        }
                    })

                } catch (e: java.lang.Exception) {
                    set_max_temperature.isEnabled = true
                    Toast.makeText(this, "Temperature must be positive", Toast.LENGTH_LONG).show()
                }
                true
            } else {
                false
            }
        }

        outer_container.setOnClickListener {
            hideKeyboard()
        }
    }


    fun updateThresholdLine() {
        var threshold = 0
        var maxTemp = 0
        try {
            threshold = set_watering_threshold.text.toString().toInt()
            maxTemp = set_max_temperature.text.toString().toInt()
        } catch (e: NumberFormatException){
            return
        }
        mp_chart.axisLeft.removeAllLimitLines()

        val moistureLimit  = LimitLine(threshold.toFloat(), "Moisture threshold")
        moistureLimit.lineWidth = 4f
        moistureLimit.lineColor = getColor(R.color.colorPrimary)
        moistureLimit.enableDashedLine(10f, 10f, 0f)
        moistureLimit.textSize = 10f

        val maxTempLine  = LimitLine(maxTemp.toFloat(), "Maximum watering temperature")
        maxTempLine.lineWidth = 4f
        maxTempLine.lineColor = getColor(R.color.tempColor)
        maxTempLine.enableDashedLine(10f, 10f, 0f)
        maxTempLine.textSize = 10f

        mp_chart.axisLeft.addLimitLine(moistureLimit)
        mp_chart.axisLeft.addLimitLine(maxTempLine)
        mp_chart.invalidate()
    }

    override fun updateTimeViews(lastUpdate: Long, nextUpdate: Long) {
        val date = Date(lastUpdate)
        val format = SimpleDateFormat("HH:mm:ss")
        latest_timestamp.text = "${getTimeAgo(lastUpdate)} (${format.format(date)})"
        time_until_next.text = getTimeUntil(nextUpdate)
    }


    override fun updatePlot(sensorData: List<WateringData>) {
        val gaveWaterEntries = sensorData.filter { it.wateredPlant }.map { Entry(it.time.toFloat(), it.moisture.toFloat()) }
        val moistureEntries = sensorData.map { Entry(it.time.toFloat(), it.moisture.toFloat()) }
        val humidityEntries = sensorData.map { Entry(it.time.toFloat(), it.humidity.toFloat()) }
        val tempEntries = sensorData.map { Entry(it.time.toFloat(), it.temperature) }

        val moistureDataSet = LineDataSet(moistureEntries, "Soil moisture")
        val humidityDataSet = LineDataSet(humidityEntries, "Humidity")
        val tempDataSet = LineDataSet(tempEntries, "Temperature")
        val gaveWaterDataSet = LineDataSet(gaveWaterEntries, "Gave water")

        moistureDataSet.color = getColor(R.color.colorPrimary)
        moistureDataSet.lineWidth = 3f
        moistureDataSet.setDrawValues(false)
        moistureDataSet.setDrawCircles(false)
        moistureDataSet.setDrawCircleHole(false)

        gaveWaterDataSet.circleRadius = 5f
        gaveWaterDataSet.setColor(getColor(R.color.colorAccent))
        gaveWaterDataSet.setDrawValues(false)
        gaveWaterDataSet.setCircleColor(getColor(R.color.colorAccent))
        gaveWaterDataSet.lineWidth = 0f
        gaveWaterDataSet.enableDashedLine(0f,1000f,3f)
        gaveWaterDataSet.setDrawCircleHole(false)

        humidityDataSet.color = getColor(R.color.humidityColor)
        humidityDataSet.setDrawCircleHole(false)
        humidityDataSet.setDrawCircles(false)
        humidityDataSet.setDrawValues(false)
        humidityDataSet.lineWidth = 3f

        tempDataSet.color = getColor(R.color.tempColor)
        tempDataSet.setDrawCircleHole(false)
        tempDataSet.setDrawCircles(false)
        tempDataSet.setDrawValues(false)
        tempDataSet.lineWidth = 3f

        val xAxis = mp_chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = object : IndexAxisValueFormatter() {

            override fun getFormattedValue(value: Float): String {
                return SimpleDateFormat("HH:mm MM-dd").format(Date(value.toLong())).toString()
            }

            override fun getPointLabel(entry: Entry?): String {
                return SimpleDateFormat("HH:mm MM-dd").format(Date(entry!!.x.toLong())).toString()
            }
        }


        xAxis.labelRotationAngle = 45f

        mp_chart.isHighlightPerTapEnabled = true
        mp_chart.isDoubleTapToZoomEnabled = false
        mp_chart.setDrawGridBackground(false)

        mp_chart.rendererRightYAxis
        mp_chart.marker = CustomMarkerView(this, sensorData, mp_chart)
        mp_chart.data = LineData(moistureDataSet, humidityDataSet, tempDataSet, gaveWaterDataSet)
        mp_chart.invalidate()
        mp_chart.description.isEnabled = false

        mp_chart.zoom(1f,1f,0f,0f)
        mp_chart.moveViewToX((System.currentTimeMillis()).toFloat())

        updateThresholdLine()
    }

    override fun updateLatestData(sensorData: WateringData) {
        latest_temperature.text = sensorData.temperature.toString()
        latest_humidity.text = sensorData.humidity.toString()
        latest_moisture.text = sensorData.moisture.toString()

        watered.text = when (sensorData.wateredPlant) {
            true -> "Yes"
            else -> "No"
        }
    }

    override fun updateWateredAmount(amount: Double) {
        watered_ammount.text = String.format("%.1fl", amount)
    }

    override fun setNotificationsEnabled(enabled: Boolean) {
        enable_notifications_chip.isChecked = enabled
    }

    override fun setEnabled(enabled: Boolean) {
        enable_watering_chip.isChecked = enabled
    }

    override fun setForceNext(forceNext: Boolean) {
        force_next.isChecked = forceNext
    }

    override fun setThreshold(threshold: Int) {
        set_watering_threshold.setText(threshold.toString())
        updateThresholdLine()
    }

    override fun setWateringAmount(amount: Int) {
        set_watering_amount.setText(amount.toString())
    }

    override fun setUpdateFrequency(hours: Double) {
        set_update_frequency.setText(hours.toString())
    }

    override fun setMaxWateringTemperature(maxTemperature: Int) {
        set_max_temperature.setText(maxTemperature.toString())
        updateThresholdLine()
    }


    private fun hideKeyboard() {
        val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        var view = currentFocus
        if (view == null) {
            view = View(this);
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0);
        view.clearFocus()
    }

}







