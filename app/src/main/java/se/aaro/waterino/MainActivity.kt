package se.aaro.waterino

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.launch
import se.aaro.waterino.data.ui.WateringData
import se.aaro.waterino.signin.SignInActivity
import se.aaro.waterino.utils.createChartLineData
import se.aaro.waterino.view.CustomMarkerView
import se.aaro.waterino.wateringdata.WateringDataViewModel
import se.aaro.waterino.wateringdata.WateringDataViewModel.UiAction
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: WateringDataViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SensorDataScreen()
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
        WaterinoTheme {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(scrollState)
            ) {
                ExpandableCard("Settings") {
                    SettingsToggle(
                        title = "Push notifications",
                        checked = viewModel.uiState.value.settingsState.pushNotificationsEnabled
                    ) { performAction(UiAction.SetPushNotificationsEnabled(it)) }

                    SettingsToggle(
                        title = "Enable Waterino",
                        checked = viewModel.uiState.value.settingsState.waterinoEnabled
                    ) { performAction(UiAction.SetWaterinoEnabled(it)) }

                    SettingsToggle(
                        title = "Force next watering",
                        checked = viewModel.uiState.value.settingsState.forceNextWatering
                    ) { performAction(UiAction.SetForceNextWateringEnabled(it)) }

                    ResetDataRow(
                        lastDataReset = viewModel.formatResetString(
                            viewModel.uiState.value.settingsState.lastDataReset
                        )
                    ) { performAction(UiAction.ResetData) }

                    IntInputField(
                        hint = "VWC Watering threshold [%]",
                        value = viewModel.uiState.value.settingsState.wateringThreshold.toString()
                    ) {
                        performAction(
                            action = UiAction.SetWateringThreshold(it),
                            successMessage = "Set watering threshold to $it%"
                        )
                    }

                    DoubleInputField(
                        hint = "Update frequency [hours]",
                        value = viewModel.uiState.value.settingsState.updateFrequency.toString()
                    ) {
                        performAction(
                            action = UiAction.SetUpdateFrequencyHours(it),
                            successMessage = "Set update frequency to $it hours"
                        )
                    }
                    IntInputField(
                        hint = "Watering volume [ml]",
                        value = viewModel.uiState.value.settingsState.wateringVolumeMl.toString()
                    ) {
                        performAction(
                            action = UiAction.SetWateringVolumeMl(it),
                            successMessage = "Set watering volume to $it ml"
                        )
                    }
                    IntInputField(
                        hint = "Maximum watering temperature [°C]",
                        value = viewModel.uiState.value.settingsState.maxWateringTemperature.toString()
                    ) {
                        performAction(
                            action = UiAction.SetWateringThreshold(it),
                            successMessage = "Set maximum watering temperature to $it°C"
                        )
                    }

                }
                Spacer(Modifier.height(16.dp))
                SectionCard("Latest data") {
                    LatestDataRow(
                        title = "Last updated",
                        value = viewModel.uiState.value.currentPlantState.lastUpdated
                    )
                    LatestDataRow(
                        title = "VWC",
                        value = viewModel.uiState.value.currentPlantState.soilMoisture
                    )
                    LatestDataRow(
                        title = "Temperature",
                        value = viewModel.uiState.value.currentPlantState.temperature
                    )
                    LatestDataRow(
                        title = "Humidity",
                        value = viewModel.uiState.value.currentPlantState.humidity
                    )
                    LatestDataRow(
                        title = "Watered",
                        value = viewModel.uiState.value.currentPlantState.gaveWater
                    )
                    Spacer(Modifier.height(8.dp))
                    LatestDataRow(
                        title = "Approximated watered amount",
                        value = viewModel.uiState.value.currentPlantState.totalWateredAmount,
                        fontSize = AppTypography.labelSmall.fontSize
                    )
                    LatestDataRow(
                        title = "Next measurement",
                        value = viewModel.uiState.value.currentPlantState.nextUpdate,
                        fontSize = AppTypography.labelSmall.fontSize
                    )
                }
                Spacer(Modifier.height(16.dp))
                SensorDataPlot(
                    sensorData = viewModel.uiState.value.wateringData,
                    wateringThreshold = viewModel.uiState.value.settingsState.wateringThreshold,
                    maxWateringTemperature = viewModel.uiState.value.settingsState.maxWateringTemperature
                )
            }
        }
    }

    private fun performAction(action: UiAction, successMessage: String? = null) {
        lifecycleScope.launch {
            viewModel.onUiAction(action)
                .apply {
                    exceptionOrNull()?.let {
                        showToast(it.localizedMessage ?: "Something went wrong")
                    } ?: run {
                        successMessage?.let { showToast(it) }
                    }
                }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
    @Composable
    fun ExpandableCard(
        title: String,
        content: @Composable ColumnScope.() -> Unit = { }
    ) {
        var expandedState by remember { mutableStateOf(false) }
        val expandedIconRotation: Float by animateFloatAsState(if (expandedState) 180f else 0f)
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = { expandedState = !expandedState }
        ) {
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
                                md_theme_light_primary,
                                BlendMode.SrcAtop
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
        title: String,
        content: @Composable ColumnScope.() -> Unit = { }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(Modifier.padding(16.dp)) {
                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight(500)
                    )
                    Spacer(Modifier.height(16.dp))
                    Column(content = content)
                }
            }
        }
    }

    @Composable
    fun LatestDataRow(
        title: String,
        value: String,
        fontSize: TextUnit = AppTypography.labelLarge.fontSize
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
    fun SettingsToggle(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit = {}) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$title:",
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }

    @Composable
    fun ResetDataRow(lastDataReset: String, onReset: () -> Unit = {}) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Text(text = "Last data reset:")
                Text(
                    text = "$lastDataReset",
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            Button(onReset) {
                Text("Reset")
            }
        }
    }

    @Composable
    fun IntInputField(hint: String, value: String, onEnter: (Int) -> Unit = {}) {
        var text by remember { mutableStateOf(value) }
        val focusManager = LocalFocusManager.current
        OutlinedTextField(
            value = text,
            label = { Text(hint) },
            onValueChange = { newValue -> text = newValue.filter { it.isDigit() } },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    try {
                        val newIntValue = text.toInt()
                        if (newIntValue < 0) throw IllegalArgumentException()
                        onEnter(newIntValue)
                        focusManager.clearFocus()
                    } catch (numberFormatException: NumberFormatException) {
                        showToast("Expecting a positive integer")
                    }
                }
            )
        )
    }

    @Composable
    fun DoubleInputField(hint: String, value: String, onEnter: (Double) -> Unit = {}) {
        var textFieldValue by remember { mutableStateOf(value) }
        textFieldValue = value
        val focusManager = LocalFocusManager.current
        OutlinedTextField(
            value = textFieldValue,
            label = { Text(hint) },
            onValueChange = { newValue ->
                textFieldValue = newValue.filter { it.isDigit() || it == '.' }
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    try {
                        val newDoubleValue = textFieldValue.toDouble()
                        if (newDoubleValue < 0) throw IllegalArgumentException()
                        onEnter(newDoubleValue)
                        focusManager.clearFocus()
                    } catch (numberFormatException: NumberFormatException) {
                        showToast("Expecting a positive double")
                    }
                }
            )
        )
    }

    @Composable
    fun SensorDataPlot(
        sensorData: List<WateringData>,
        wateringThreshold: Int,
        maxWateringTemperature: Int
    ) {
        val chartView = remember { mutableStateOf<LineChart?>(null) }
        chartView.value?.let { lineCart ->
            updatePlot(this, sensorData, lineCart)
            updateThresholdLines(lineCart, maxWateringTemperature, wateringThreshold)
        }
        SectionCard("Plot") {
            AndroidView(
                modifier = Modifier
                    .height(300.dp)
                    .fillMaxWidth(),
                factory = { context ->
                    if (chartView.value == null) chartView.value = LineChart(context)
                    return@AndroidView chartView.value!!
                })
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun updateThresholdLines(
        lineChart: LineChart,
        maxWateringTemperature: Int,
        wateringThreshold: Int
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
        context: Context,
        sensorData: List<WateringData>,
        lineChart: LineChart
    ) {
        val xAxis = lineChart.xAxis
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
                        "Approximated watered amount",
                        "0.0l",
                        AppTypography.labelSmall.fontSize
                    )
                    LatestDataRow(
                        "Next measurement",
                        "3 minutes",
                        AppTypography.labelSmall.fontSize
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

}