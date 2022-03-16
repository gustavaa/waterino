package se.aaro.waterino

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.animation.Animation
import android.view.animation.AnticipateOvershootInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import se.aaro.waterino.data.ui.WateringData
import se.aaro.waterino.databinding.ActivityMainBinding
import se.aaro.waterino.signin.SignInActivity
import se.aaro.waterino.utils.collapse
import se.aaro.waterino.utils.expand
import se.aaro.waterino.view.CustomMarkerView
import se.aaro.waterino.wateringdata.WateringDataViewModel
import se.aaro.waterino.wateringdata.WateringDataViewModel.UiAction
import se.aaro.waterino.wateringdata.WateringDataViewModel.UiState
import java.text.SimpleDateFormat
import java.util.*


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private val viewModel: WateringDataViewModel by viewModels()
    var isSettingsExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        setUpViews()
        viewModel.uiState
            .onEach { updateViews(it) }
            .launchIn(lifecycleScope)
    }

    override fun onResume() {
        super.onResume()
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user == null) {
            val signInIntent = Intent(this, SignInActivity::class.java)
            startActivity(signInIntent)
        }
    }

    private fun setUpViews() {
        binding.apply {
            enableNotificationsChip.setOnCheckedChangeListener { _, isChecked ->
                lifecycleScope.launch {
                    viewModel.onUiAction(UiAction.SetPushNotificationsEnabled(isChecked))
                }
            }
            forceNext.setOnCheckedChangeListener { _, isChecked ->
                lifecycleScope.launch {
                    viewModel.onUiAction(UiAction.SetForceNextWateringEnabled(isChecked)).apply {
                        exceptionOrNull()?.let {
                            showToast(it.localizedMessage ?: "Something went wrong")
                            updateViews(viewModel.uiState.value)
                        }
                    }
                }
            }
            enableWateringChip.setOnCheckedChangeListener { _, isChecked ->
                lifecycleScope.launch {
                    viewModel.onUiAction(UiAction.SetWaterinoEnabled(isChecked)).apply {
                        exceptionOrNull()?.let {
                            showToast(it.localizedMessage ?: "Something went wrong")
                            updateViews(viewModel.uiState.value)
                        }
                    }
                }
            }
            resetDataButton.setOnClickListener {
                lifecycleScope.launch {
                    viewModel.onUiAction(UiAction.ResetData).apply {
                        if (isSuccess) {
                            showToast("Reset watering data")
                        } else {
                            showToast(exceptionOrNull()?.localizedMessage ?: "Something went wrong")
                            updateViews()
                        }
                    }
                }
            }
            setWateringThreshold.onIntValueEntered { newValue ->
                lifecycleScope.launch {
                    viewModel.onUiAction(UiAction.SetWateringThreshold(newValue)).apply {
                        exceptionOrNull()?.let {
                            showToast(it.localizedMessage ?: "Something went wrong")
                            updateViews()
                        } ?: showToast("Set watering threshold to $newValue%")
                        setWateringThreshold.isEnabled = true
                    }
                }
            }
            setUpdateFrequency.onDoubleValueEntered { newValue ->
                lifecycleScope.launch {
                    viewModel.onUiAction(UiAction.SetUpdateFrequencyHours(newValue)).apply {
                        exceptionOrNull()?.let {
                            showToast(it.localizedMessage ?: "Something went wrong")
                            updateViews()
                        } ?: showToast("Set update frequency to $newValue hours")
                        setUpdateFrequency.isEnabled = true
                    }
                }
            }
            setWateringVolume.onIntValueEntered { newValue ->
                lifecycleScope.launch {
                    viewModel.onUiAction(UiAction.SetWateringVolumeMl(newValue)).apply {
                        exceptionOrNull()?.let {
                            showToast(it.localizedMessage ?: "Something went wrong")
                            updateViews()
                        } ?: showToast("Set watering volume to $newValue ml")
                        setWateringVolume.isEnabled = true
                    }
                }
            }
            setMaxTemperature.onIntValueEntered { newValue ->
                lifecycleScope.launch {
                    viewModel.onUiAction(UiAction.SetMaximumWateringTemperature(newValue)).apply {
                        exceptionOrNull()?.let {
                            showToast(it.localizedMessage ?: "Something went wrong")
                            updateViews()
                        } ?: showToast("Set maximum watering temperature to $newValue°C")
                        setMaxTemperature.isEnabled = true
                    }
                }
            }
            settingsContainer.collapse()
            settingsCard.setOnClickListener { v ->
                when (isSettingsExpanded) {
                    true -> settingsContainer.collapse(object : Animation.AnimationListener {
                        override fun onAnimationRepeat(p0: Animation?) {}
                        override fun onAnimationStart(p0: Animation?) {
                            expandCollapseButton.animate().rotation(0F).setDuration(400)
                                .setInterpolator(AnticipateOvershootInterpolator()).start()
                        }

                        override fun onAnimationEnd(p0: Animation?) {
                            isSettingsExpanded = false
                        }
                    })
                    false -> settingsContainer.expand(object : Animation.AnimationListener {
                        override fun onAnimationRepeat(p0: Animation?) {}
                        override fun onAnimationStart(p0: Animation?) {
                            expandCollapseButton.animate().rotation(180F).setDuration(400)
                                .setInterpolator(AnticipateOvershootInterpolator()).start()
                        }

                        override fun onAnimationEnd(p0: Animation?) {
                            isSettingsExpanded = true
                        }
                    })
                }
            }
        }
    }

    private fun updateViews(uiState: UiState = viewModel.uiState.value) {
        binding.apply {

            uiState.settingsState.apply {
                enableNotificationsChip.isChecked = pushNotificationsEnabled
                enableWateringChip.isChecked = waterinoEnabled
                forceNext.isChecked = forceNextWatering

                lastResetButton.text = lastDataReset
                setWateringThreshold.setText(wateringThreshold.toString())
                setUpdateFrequency.setText(updateFrequency.toString())
                setWateringVolume.setText(wateringVolumeMl.toString())
                setMaxTemperature.setText(maxWateringTemperature.toString())
            }

            uiState.currentPlantState.apply {
                latestTimestamp.text = lastUpdated
                latestMoisture.text = soilMoisture
                latestTemperature.text = temperature
                latestHumidity.text = humidity
                watered.text = gaveWater
                wateredAmmount.text = totalWateredAmount
                timeUntilNext.text = nextUpdate
            }

            uiState.wateringData.apply {
                updatePlot(this)
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun updateThresholdLine() {
        binding.apply {
            var threshold = 0
            var maxTemp = 0
            try {
                threshold = setWateringThreshold.text.toString().toInt()
                maxTemp = setMaxTemperature.text.toString().toInt()
            } catch (e: NumberFormatException) {
                return
            }
            binding.mpChart.axisLeft.removeAllLimitLines()

            val moistureLimit = LimitLine(threshold.toFloat(), "Moisture threshold")
            moistureLimit.lineWidth = 4f
            moistureLimit.lineColor = getColor(R.color.colorPrimary)
            moistureLimit.enableDashedLine(10f, 10f, 0f)
            moistureLimit.textSize = 10f

            val maxTempLine = LimitLine(maxTemp.toFloat(), "Maximum watering temperature")
            maxTempLine.lineWidth = 4f
            maxTempLine.lineColor = getColor(R.color.tempColor)
            maxTempLine.enableDashedLine(10f, 10f, 0f)
            maxTempLine.textSize = 10f

            mpChart.axisLeft.addLimitLine(moistureLimit)
            mpChart.axisLeft.addLimitLine(maxTempLine)
            mpChart.invalidate()
        }
    }

    private fun updatePlot(sensorData: List<WateringData>) {
        val gaveWaterEntries = sensorData.filter { it.gaveWater }
            .map { Entry(it.time.toFloat(), it.moisture) }
        val moistureEntries = sensorData.map { Entry(it.time.toFloat(), it.moisture) }
        val humidityEntries = sensorData.map { Entry(it.time.toFloat(), it.humidity) }
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
        gaveWaterDataSet.color = getColor(R.color.colorAccent)
        gaveWaterDataSet.setDrawValues(false)
        gaveWaterDataSet.setCircleColor(getColor(R.color.colorAccent))
        gaveWaterDataSet.lineWidth = 0f
        gaveWaterDataSet.enableDashedLine(0f, 1000f, 3f)
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

        val xAxis = binding.mpChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = object : IndexAxisValueFormatter() {

            override fun getFormattedValue(value: Float): String {
                return SimpleDateFormat(
                    "HH:mm MM-dd",
                    Locale.getDefault()
                ).format(Date(value.toLong())).toString()
            }

            override fun getPointLabel(entry: Entry?): String {
                return SimpleDateFormat(
                    "HH:mm MM-dd",
                    Locale.getDefault()
                ).format(Date(entry!!.x.toLong())).toString()
            }
        }

        xAxis.labelRotationAngle = 45f

        binding.mpChart.isHighlightPerTapEnabled = true
        binding.mpChart.isDoubleTapToZoomEnabled = false
        binding.mpChart.setDrawGridBackground(false)

        binding.mpChart.rendererRightYAxis
        binding.mpChart.marker = CustomMarkerView(this, sensorData, binding.mpChart)
        binding.mpChart.data =
            LineData(moistureDataSet, humidityDataSet, tempDataSet, gaveWaterDataSet)
        binding.mpChart.invalidate()
        binding.mpChart.description.isEnabled = false

        binding.mpChart.zoom(1f, 1f, 0f, 0f)

        updateThresholdLine()
    }

    private fun EditText.onIntValueEntered(onIntValueEntered: (Int) -> Unit) {
        this.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                try {
                    this.isEnabled = false
                    val newIntValue = this.text.toString().toInt()
                    if (newIntValue < 0) throw IllegalArgumentException()
                    onIntValueEntered(newIntValue)
                } catch (e: java.lang.Exception) {
                    this.isEnabled = true
                    showToast("Expecting a positive integer")
                    updateViews()
                }
                true
            } else {
                false
            }
        }
    }

    private fun EditText.onDoubleValueEntered(onDoubleValueEntered: (Double) -> Unit) {
        this.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                try {
                    this.isEnabled = false
                    val newDoubleValue = this.text.toString().toDouble()
                    if (newDoubleValue < 0) throw IllegalArgumentException()
                    onDoubleValueEntered(newDoubleValue)
                } catch (e: java.lang.Exception) {
                    this.isEnabled = true
                    showToast("Expecting a positive decimal number")
                    updateViews()
                }
                true
            } else {
                false
            }
        }
    }
}







