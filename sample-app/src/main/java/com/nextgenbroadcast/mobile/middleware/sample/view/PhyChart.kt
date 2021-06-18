package com.nextgenbroadcast.mobile.middleware.sample.view

import android.content.Context
import android.util.AttributeSet
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.nextgenbroadcast.mobile.middleware.sample.R
import com.nextgenbroadcast.mobile.telemetry.TelemetryManager
import kotlinx.android.synthetic.main.fragment_main.view.*
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

class PhyChart @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : GraphView(context, attrs, defStyleAttr) {

    private var phyGraphSeries: LineGraphSeries<DataPoint> = LineGraphSeries()
    private var phyEventCounter = 0
    private var eventStartTime: Long = 0
    private var currentSpeed: Long = 0

    override fun onFinishInflate() {
        super.onFinishInflate()
        val currentTime = System.currentTimeMillis().toDouble()
        with(phyChart.viewport) {
            isXAxisBoundsManual = true
            setMaxX(currentTime)
            setMinX((currentTime - TIME_PERIOD_FOR_ACTUAL_SNR_MILLISECOND))
            isYAxisBoundsManual = true
            setMinY(SNR_MIN_VALUE)
            setMaxY(SNR_MAX_VALUE)
            isYAxisBoundsManual = true
            setMinY(SNR_MIN_VALUE)
            setMaxY(SNR_MAX_VALUE)
            isScrollable = true
        }

        phyChart.title = resources.getString(R.string.phy_snr)
        phyChart.titleColor = resources.getColor(R.color.white)
        phyChart.gridLabelRenderer.gridColor = resources.getColor(R.color.white)
        phyChart.gridLabelRenderer.numVerticalLabels = 4
        phyChart.addSeries(phyGraphSeries)
    }

    fun addEvent(phyPayload: TelemetryManager.PhyPayload) {
        val currentTime = System.currentTimeMillis()
        if (phyEventCounter == 0) {
            eventStartTime = currentTime
        }

        if (phyEventCounter >= SELECTIONS_FOR_CALCULATE_AVERAGE_SPEED) {
            currentSpeed = ((currentTime - eventStartTime) / SELECTIONS_FOR_CALCULATE_AVERAGE_SPEED)
            phyEventCounter = 0
        }

        val dataPoint =
            DataPoint(
                currentTime.toDouble(),
                min(max(SNR_MIN_VALUE, phyPayload.snr1000.toDouble()), SNR_MAX_VALUE)
            )

        val maxValue = if (currentSpeed == 0L) {
            INITIAL_SNR_MAX_VALUE
        } else {
            TIME_PERIOD_FOR_ACTUAL_SNR_MILLISECOND / currentSpeed
        }

        phyChart.viewport.setMaxX(currentTime.toDouble())
        phyChart.viewport.setMinX((currentTime - TIME_PERIOD_FOR_ACTUAL_SNR_MILLISECOND).toDouble())
        phyGraphSeries.appendData(dataPoint, true, maxValue.toInt())
        phyEventCounter++
    }

    companion object {
        private const val SELECTIONS_FOR_CALCULATE_AVERAGE_SPEED = 10
        private const val INITIAL_SNR_MAX_VALUE = 100
        private const val SNR_MAX_VALUE = 30.0
        private const val SNR_MIN_VALUE = 0.0
        private const val TIME_PERIOD_FOR_ACTUAL_SNR_MINUTES = 5L
        private val TIME_PERIOD_FOR_ACTUAL_SNR_MILLISECOND =
            TimeUnit.MINUTES.toMillis(TIME_PERIOD_FOR_ACTUAL_SNR_MINUTES)

    }
}