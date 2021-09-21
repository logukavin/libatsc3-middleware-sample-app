package com.nextgenbroadcast.mobile.middleware.scoreboard.view

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.nextgenbroadcast.mobile.middleware.dev.chart.TemporalChartView
import com.nextgenbroadcast.mobile.middleware.scoreboard.R
import com.nextgenbroadcast.mobile.middleware.scoreboard.entities.TDataPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

class PhyChart @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : TemporalChartView(context, attrs, defStyleAttr) {

    override fun onFinishInflate() {
        super.onFinishInflate()

        title = resources.getString(R.string.chart_phy_snr_title)
        val textColor = ContextCompat.getColor(context, R.color.white)

        titleColor = textColor
        with(gridLabelRenderer) {
            gridColor = ContextCompat.getColor(context, R.color.chart_grid_color)
            verticalLabelsColor = textColor
            horizontalLabelsColor = textColor
            textSize = resources.getDimension(R.dimen.chart_label_text_size)
            numVerticalLabels = 4
        }

        setViewport(VISIBLE_PERIOD, SNR_MIN_VALUE, SNR_MAX_VALUE)
    }

    class DataSource(
        private val flow: Flow<TDataPoint>,
    ) : TemporalDataSource(VISIBLE_PERIOD) {
        private var graphSeries = LineGraphSeries<DataPoint>()
        private var sourceJob: Job? = null

        override fun open() {
            super.open()

            sourceJob = CoroutineScope(Dispatchers.Main).launch {
                flow.collect { (time, value) ->
                    addValue(graphSeries, min(max(SNR_MIN_VALUE, value / 1000), SNR_MAX_VALUE), time)
                }
            }
        }

        override fun close(reset: Boolean) {
            super.close(reset)

            sourceJob?.cancel()
            sourceJob = null

            if (reset) {
                graphSeries.resetData(emptyArray())
            }
        }

        override fun getSeries() = listOf(graphSeries)
    }

    companion object {
        private const val SNR_MAX_VALUE = 30.0
        private const val SNR_MIN_VALUE = 0.0

        private val VISIBLE_PERIOD = TimeUnit.MINUTES.toMillis(5)
    }
}