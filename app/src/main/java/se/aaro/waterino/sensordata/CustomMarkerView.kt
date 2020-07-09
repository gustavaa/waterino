package se.aaro.waterino.sensordata

import android.content.Context
import android.graphics.Canvas
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import kotlinx.android.synthetic.main.marker_view.view.*
import se.aaro.waterino.R
import java.text.SimpleDateFormat
import java.util.*

class CustomMarkerView(context: Context, val chartData: List<WateringData?>, val chart: LineChart): MarkerView(context, R.layout.marker_view) {


    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        val dataPoint = chartData.find { it!!.time.toFloat() == e!!.x }
        println(e!!.x)
        date.text =  SimpleDateFormat("HH:mm MM-dd").format(Date(dataPoint!!.time)).toString()
        marker_humidity.text = String.format("%d", dataPoint?.humidity)
        marker_moisture.text = String.format("%d", dataPoint?.moisture)
        marker_temperature.text = String.format("%.1fC", dataPoint?.temperature)
        chartView = chart
    }

    override fun draw(canvas: Canvas?, posX: Float, posY: Float) {
        super.draw(canvas, posX, posY)
        getOffsetForDrawingAtPoint(posX,posY)
    }


}