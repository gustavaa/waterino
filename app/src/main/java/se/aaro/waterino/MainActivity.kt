package se.aaro.waterino

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.compose.WaterinoTheme
import com.example.compose.md_theme_light_primary
import com.example.ui.theme.AppTypography
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import se.aaro.waterino.data.ui.WateringData
import se.aaro.waterino.data.ui.WateringMode
import se.aaro.waterino.signin.SignInActivity
import se.aaro.waterino.utils.createChartLineData
import se.aaro.waterino.utils.getTimeAgo
import se.aaro.waterino.utils.getTimeUntil
import se.aaro.waterino.view.CustomMarkerView
import se.aaro.waterino.wateringdata.WateringDataViewModel
import se.aaro.waterino.wateringdata.WateringDataViewModel.UiAction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: WateringDataViewModel by viewModels()
    private var lastUpdated by mutableStateOf("-")
    private var timeUntilNextMeasurement by mutableStateOf("-")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SensorDataScreen()
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (isActive) {
                    lastUpdated = calculateTimeSinceLastUpdate()
                    timeUntilNextMeasurement = calculateTimeUntilNextMeasurement()
                    delay(15000)
                }
            }
        }
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

    @Composable
    fun SensorDataScreen() {
        val scrollState = rememberScrollState(0)
        val uiState by viewModel.uiState.collectAsState()

        LaunchedEffect(uiState.currentPlantState.lastUpdated) {
            lastUpdated = calculateTimeSinceLastUpdate()
            timeUntilNextMeasurement = calculateTimeUntilNextMeasurement()
        }

        WaterinoTheme {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(scrollState)
            ) {
                ExpandableCard("Settings") {
                    SettingsToggle(
                        title = "Push notifications",
                        checked = uiState.settingsState.pushNotificationsEnabled,
                    ) { performAction(UiAction.SetPushNotificationsEnabled(it)) }

                    SettingsToggle(
                        title = "Enable Waterino", checked = uiState.settingsState.waterinoEnabled,
                    ) { performAction(UiAction.SetWaterinoEnabled(it)) }

                    WateringModeRadioSelector(uiState)

                    ResetDataRow(
                        lastDataReset = viewModel.formatResetString(
                            uiState.settingsState.lastDataReset
                        )
                    ) { performAction(UiAction.ResetData) }

                    DoubleInputField(
                        hint = "Update frequency [hours]",
                        value = uiState.settingsState.updateFrequency.toString(),
                    ) {
                        performAction(
                            action = UiAction.SetUpdateFrequencyHours(it),
                            successMessage = "Set update frequency to $it hours"
                        )
                    }

                    IntInputField(
                        hint = "Watering volume [ml]",
                        value = uiState.settingsState.wateringVolumeMl.toString()
                    ) {
                        performAction(
                            action = UiAction.SetWateringVolumeMl(it),
                            successMessage = "Set watering volume to $it ml"
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = "Automatic watering settings: ",
                        style = MaterialTheme.typography.labelLarge
                    )

                    SettingsToggle(
                        title = "Force next watering",
                        checked = uiState.settingsState.forceNextWatering,
                        enabled = uiState.settingsState.wateringMode == WateringMode.AUTOMATIC
                    ) { performAction(UiAction.SetForceNextWateringEnabled(it)) }


                    IntInputField(
                        hint = "VWC Watering threshold [%]",
                        value = uiState.settingsState.wateringThreshold.toString(),
                        enabled = uiState.settingsState.wateringMode == WateringMode.AUTOMATIC
                    ) {
                        performAction(
                            action = UiAction.SetWateringThreshold(it),
                            successMessage = "Set watering threshold to $it%"
                        )
                    }

                    IntInputField(
                        hint = "Watering volume [ml]",
                        value = uiState.settingsState.wateringVolumeMl.toString(),
                        enabled = uiState.settingsState.wateringMode == WateringMode.AUTOMATIC
                    ) {
                        performAction(
                            action = UiAction.SetWateringVolumeMl(it),
                            successMessage = "Set watering volume to $it ml"
                        )
                    }
                    IntInputField(
                        hint = "Maximum watering temperature [°C]",
                        value = uiState.settingsState.maxWateringTemperature.toString(),
                        enabled = uiState.settingsState.wateringMode == WateringMode.AUTOMATIC
                    ) {
                        performAction(
                            action = UiAction.SetMaximumWateringTemperature(it),
                            successMessage = "Set maximum watering temperature to $it°C"
                        )
                    }

                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = "Fixed frequency watering settings: ",
                        style = MaterialTheme.typography.labelLarge
                    )
                    DoubleInputField(
                        hint = "Watering frequency [hours]",
                        value = uiState.settingsState.fixedWateringFrequencyHours.toString(),
                        enabled = uiState.settingsState.wateringMode == WateringMode.FIXED_FREQUENCY
                    ) {
                        performAction(
                            action = UiAction.SetFixedWateringFrequencyHours(it),
                            successMessage = "Set fixed watering frequency to $it hours"
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                SectionCard("Latest data") {
                    LatestDataRow(
                        title = "Last updated",
                        value = lastUpdated
                    )
                    LatestDataRow(
                        title = "VWC", value = uiState.currentPlantState.soilMoisture
                    )
                    LatestDataRow(
                        title = "Temperature", value = uiState.currentPlantState.temperature
                    )
                    LatestDataRow(
                        title = "Humidity", value = uiState.currentPlantState.humidity
                    )
                    LatestDataRow(
                        title = "Watered", value = uiState.currentPlantState.gaveWater
                    )
                    Spacer(Modifier.height(8.dp))
                    LatestDataRow(
                        title = "Approximated watered amount",
                        value = uiState.currentPlantState.totalWateredAmount,
                        fontSize = AppTypography.labelSmall.fontSize
                    )
                    LatestDataRow(
                        title = "Next measurement",
                        value = timeUntilNextMeasurement,
                        fontSize = AppTypography.labelSmall.fontSize
                    )
                }
                Spacer(Modifier.height(16.dp))
                SensorDataPlot(
                    sensorData = uiState.wateringData,
                    wateringThreshold = uiState.settingsState.wateringThreshold,
                    maxWateringTemperature = uiState.settingsState.maxWateringTemperature
                )
            }
        }
    }

    private fun calculateTimeSinceLastUpdate(): String {
        return viewModel.uiState.value.currentPlantState.lastUpdated?.let { getTimeAgo(it) } ?: "-"
    }

    private fun calculateTimeUntilNextMeasurement(): String {
        return viewModel.uiState.value.currentPlantState.nextUpdate?.let { getTimeUntil(it) } ?: "-"
    }

    @Composable
    private fun WateringModeRadioSelector(uiState: WateringDataViewModel.UiState) {
        Column {
            Text(
                text = "Watering mode:",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Automatic",
                        style = MaterialTheme.typography.labelMedium
                    )
                    RadioButton(
                        selected = uiState.settingsState.wateringMode == WateringMode.AUTOMATIC,
                        onClick = {
                            performAction(UiAction.SetWateringMode(WateringMode.AUTOMATIC))
                        }
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Fixed frequency",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    RadioButton(
                        selected = uiState.settingsState.wateringMode == WateringMode.FIXED_FREQUENCY,
                        onClick = {
                            performAction(UiAction.SetWateringMode(WateringMode.FIXED_FREQUENCY))
                        }
                    )
                }
            }
        }
    }

    private fun performAction(action: UiAction, successMessage: String? = null) {
        lifecycleScope.launch {
            viewModel.onUiAction(action).apply {
                exceptionOrNull()?.let {
                    showToast(it.localizedMessage ?: "Something went wrong")
                } ?: run {
                    successMessage?.let { showToast(it) }
                }
            }
        }
    }

    @Composable
    fun ExpandableCard(
        title: String, content: @Composable ColumnScope.() -> Unit = { }
    ) {
        var expandedState by remember { mutableStateOf(true) }
        val expandedIconRotation: Float by animateFloatAsState(if (expandedState) 180f else 0f)
        Card(modifier = Modifier.fillMaxWidth(), onClick = { expandedState = !expandedState }) {
            Box(Modifier.padding(16.dp)) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = title,
                            modifier = Modifier.align(Alignment.CenterVertically),
                            fontWeight = FontWeight(500)
                        )
                        Image(
                            painterResource(R.drawable.ic_arrow_down_24),
                            contentDescription = "",
                            contentScale = ContentScale.Crop,
                            colorFilter = ColorFilter.tint(
                                md_theme_light_primary, BlendMode.SrcAtop
                            ),
                            modifier = Modifier
                                .height(42.dp)
                                .width(42.dp)
                                .rotate(expandedIconRotation),
                        )
                    }
                    AnimatedContent(targetState = expandedState) { isExpanded ->
                        if (isExpanded) Column(content = content)
                    }
                }
            }
        }
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    fun SectionCard(
        title: String, content: @Composable ColumnScope.() -> Unit = { }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(Modifier.padding(16.dp)) {
                Column {
                    Text(
                        text = title, fontWeight = FontWeight(500)
                    )
                    Spacer(Modifier.height(16.dp))
                    Column(content = content)
                }
            }
        }
    }

    @Composable
    fun LatestDataRow(
        title: String, value: String, fontSize: TextUnit = AppTypography.labelLarge.fontSize
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(0.6f),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "$title:",
                modifier = Modifier.align(Alignment.CenterVertically),
                fontWeight = FontWeight(500),
                fontSize = fontSize
            )
            Text(
                text = value,
                modifier = Modifier.align(Alignment.CenterVertically),
                fontSize = fontSize
            )
        }
    }


    @Composable
    fun SettingsToggle(
        title: String,
        checked: Boolean,
        enabled: Boolean = true,
        onCheckedChange: (Boolean) -> Unit = {}
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$title:",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    }

    @Composable
    fun ResetDataRow(lastDataReset: String, onReset: () -> Unit = {}) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Last data reset:",
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = lastDataReset, modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            Button(onReset) {
                Text("Reset")
            }
        }
    }

    @Composable
    fun IntInputField(
        hint: String,
        value: String,
        enabled: Boolean = true,
        onEnter: (Int) -> Unit = {}
    ) {
        var text by remember { mutableStateOf(value) }
        LaunchedEffect(value) {
            text = value
        }
        val focusManager = LocalFocusManager.current
        OutlinedTextField(
            value = text,
            label = { Text(hint) },
            enabled = enabled,
            onValueChange = { newValue -> text = newValue.filter { it.isDigit() } },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = {
                try {
                    val newIntValue = text.toInt()
                    if (newIntValue < 0) throw IllegalArgumentException()
                    onEnter(newIntValue)
                    focusManager.clearFocus()
                } catch (numberFormatException: NumberFormatException) {
                    showToast("Expecting a positive integer")
                }
            })
        )
    }

    @Composable
    fun DoubleInputField(
        hint: String,
        value: String,
        enabled: Boolean = true,
        onEnter: (Double) -> Unit = {}
    ) {
        var textFieldValue by remember { mutableStateOf(value) }
        LaunchedEffect(value) {
            textFieldValue = value
        }
        val focusManager = LocalFocusManager.current
        OutlinedTextField(
            value = textFieldValue,
            label = { Text(hint) },
            enabled = enabled,
            onValueChange = { newValue ->
                textFieldValue = newValue.filter { it.isDigit() || it == '.' }
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = {
                try {
                    val newDoubleValue = textFieldValue.toDouble()
                    if (newDoubleValue < 0) throw IllegalArgumentException()
                    onEnter(newDoubleValue)
                    focusManager.clearFocus()
                } catch (numberFormatException: NumberFormatException) {
                    showToast("Expecting a positive double")
                }
            })
        )
    }

    @Composable
    fun SensorDataPlot(
        sensorData: List<WateringData>, wateringThreshold: Int, maxWateringTemperature: Int
    ) {
        val chartView = remember { mutableStateOf<LineChart?>(null) }
        chartView.value?.let { lineCart ->
            updatePlot(this, sensorData, lineCart)
            updateThresholdLines(lineCart, maxWateringTemperature, wateringThreshold)
        }
        SectionCard("Plot") {
            AndroidView(modifier = Modifier
                .height(300.dp)
                .fillMaxWidth(), factory = { context ->
                if (chartView.value == null) chartView.value = LineChart(context)
                return@AndroidView chartView.value!!
            })
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun updateThresholdLines(
        lineChart: LineChart, maxWateringTemperature: Int, wateringThreshold: Int
    ) {
        lineChart.apply {
            axisLeft.removeAllLimitLines()

            val moistureLimit = LimitLine(wateringThreshold.toFloat(), "Moisture threshold")
            moistureLimit.lineWidth = 4f
            moistureLimit.lineColor = getColor(R.color.colorPrimary)
            moistureLimit.enableDashedLine(10f, 10f, 0f)
            moistureLimit.textSize = 10f

            val maxTempLine =
                LimitLine(maxWateringTemperature.toFloat(), "Maximum watering temperature")
            maxTempLine.lineWidth = 4f
            maxTempLine.lineColor = getColor(R.color.tempColor)
            maxTempLine.enableDashedLine(10f, 10f, 0f)
            maxTempLine.textSize = 10f

            axisLeft.addLimitLine(moistureLimit)
            axisLeft.addLimitLine(maxTempLine)
            invalidate()
        }
    }

    private fun updatePlot(
        context: Context, sensorData: List<WateringData>, lineChart: LineChart
    ) {
        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = object : IndexAxisValueFormatter() {

            override fun getFormattedValue(value: Float): String {
                return SimpleDateFormat(
                    "HH:mm MM-dd", Locale.getDefault()
                ).format(Date(value.toLong())).toString()
            }

            override fun getPointLabel(entry: Entry?): String {
                return SimpleDateFormat(
                    "HH:mm MM-dd", Locale.getDefault()
                ).format(Date(entry!!.x.toLong())).toString()
            }
        }

        xAxis.labelRotationAngle = 45f

        lineChart.isHighlightPerTapEnabled = true
        lineChart.isDoubleTapToZoomEnabled = false
        lineChart.setDrawGridBackground(false)

        lineChart.rendererRightYAxis
        lineChart.marker = CustomMarkerView(context, sensorData, lineChart)
        lineChart.data = createChartLineData(sensorData, this)
        lineChart.invalidate()
        lineChart.description.isEnabled = false

        lineChart.zoom(1f, 1f, 0f, 0f)
    }

    @Composable
    @Preview
    fun Preview() {
        WaterinoTheme {
            Column {
                ExpandableCard("Settings") {
                    SettingsToggle("Push notifications", true)
                    SettingsToggle("Enable Waterino", true)
                    SettingsToggle("Force next watering", true)
                    ResetDataRow("2022-06-04")
                    IntInputField("VWC Watering threshold [%]", "2")
                    DoubleInputField("Update frequency [hours]", "0.25")
                    IntInputField("Watering volume [ml]", "1000")
                    IntInputField("Maximum watering temperature [C]", "25")

                }
                Spacer(Modifier.height(16.dp))
                SectionCard("Latest data") {
                    LatestDataRow("Last updated", "just now")
                    LatestDataRow("VWC", "23%")
                    LatestDataRow("Temperature", "23")
                    LatestDataRow("Humidity", "69%")
                    LatestDataRow("Watered", "No")
                    Spacer(Modifier.height(8.dp))
                    LatestDataRow(
                        "Approximated watered amount", "0.0l", AppTypography.labelSmall.fontSize
                    )
                    LatestDataRow(
                        "Next measurement", "3 minutes", AppTypography.labelSmall.fontSize
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

}