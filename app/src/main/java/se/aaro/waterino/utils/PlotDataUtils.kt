package se.aaro.waterino.utils

import android.content.Context
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import se.aaro.waterino.R
import se.aaro.waterino.data.ui.WateringData


fun createChartLineData(wateringData: List<WateringData>, context: Context): LineData {
    val gaveWaterEntries = wateringData.filter { it.gaveWater }
        .map { Entry(it.time.toFloat(), it.moisture) }
    val moistureEntries = wateringData.map { Entry(it.time.toFloat(), it.moisture) }
    val humidityEntries = wateringData.map { Entry(it.time.toFloat(), it.humidity) }
    val tempEntries = wateringData.map { Entry(it.time.toFloat(), it.temperature) }

    val moistureDataSet = LineDataSet(moistureEntries, "VWC")
    val humidityDataSet = LineDataSet(humidityEntries, "Humidity")
    val tempDataSet = LineDataSet(tempEntries, "Temperature")
    val gaveWaterDataSet = LineDataSet(gaveWaterEntries, "Gave water")

    moistureDataSet.color = context.getColor(R.color.colorPrimary)
    moistureDataSet.lineWidth = 3f
    moistureDataSet.setDrawValues(false)
    moistureDataSet.setDrawCircles(false)
    moistureDataSet.setDrawCircleHole(false)

    gaveWaterDataSet.circleRadius = 5f
    gaveWaterDataSet.color = context.getColor(R.color.colorAccent)
    gaveWaterDataSet.setDrawValues(false)
    gaveWaterDataSet.setCircleColor(context.getColor(R.color.colorAccent))
    gaveWaterDataSet.lineWidth = 0f
    gaveWaterDataSet.enableDashedLine(0f, 1000f, 3f)
    gaveWaterDataSet.setDrawCircleHole(false)

    humidityDataSet.color = context.getColor(R.color.humidityColor)
    humidityDataSet.setDrawCircleHole(false)
    humidityDataSet.setDrawCircles(false)
    humidityDataSet.setDrawValues(false)
    humidityDataSet.lineWidth = 3f

    tempDataSet.color = context.getColor(R.color.tempColor)
    tempDataSet.setDrawCircleHole(false)
    tempDataSet.setDrawCircles(false)
    tempDataSet.setDrawValues(false)
    tempDataSet.lineWidth = 3f
    return LineData(moistureDataSet, humidityDataSet, tempDataSet, gaveWaterDataSet)
}
