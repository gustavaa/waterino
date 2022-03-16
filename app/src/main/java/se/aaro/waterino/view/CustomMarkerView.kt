package se.aaro.waterino.view

import android.content.Context
import android.graphics.Canvas
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import se.aaro.waterino.R
import se.aaro.waterino.data.ui.WateringData
import se.aaro.waterino.databinding.MarkerViewBinding
import java.text.SimpleDateFormat
import java.util.*

class CustomMarkerView(context: Context, val chartData: List<WateringData?>, val chart: LineChart) :
    MarkerView(context, R.layout.marker_view) {

    lateinit var binding: MarkerViewBinding

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        binding = MarkerViewBinding.bind(findViewById(R.id.custom_marker_root))
    }

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        val dataPoint = chartData.find { it!!.time.toFloat() == e!!.x }
        println(e!!.x)
        binding.date.text =
            SimpleDateFormat("HH:mm MM-dd").format(Date(dataPoint!!.time)).toString()
        binding.markerHumidity.text = String.format("%f", dataPoint.humidity)
        binding.markerMoisture.text = String.format("%f", dataPoint.moisture)
        binding.markerTemperature.text = String.format("%.1fC", dataPoint.temperature)
        chartView = chart
    }

    override fun draw(canvas: Canvas?, posX: Float, posY: Float) {
        super.draw(canvas, posX, posY)
        getOffsetForDrawingAtPoint(posX, posY)
    }


}
