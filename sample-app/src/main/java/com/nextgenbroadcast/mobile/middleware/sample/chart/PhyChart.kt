package com.nextgenbroadcast.mobile.middleware.sample.chart

import android.content.Context
import android.util.AttributeSet
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.nextgenbroadcast.mobile.middleware.sample.R
import com.nextgenbroadcast.mobile.telemetry.TelemetryClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
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
        gridLabelRenderer.numVerticalLabels = 4

        setViewport(VISIBLE_PERIOD, SNR_MIN_VALUE, SNR_MAX_VALUE)
    }

    class DataSource(
        client: TelemetryClient
    ) : TemporalDataSource(VISIBLE_PERIOD) {
        private var graphSeries = LineGraphSeries<DataPoint>()
        private val flow = client.getPayloadFlow<PhyPayload>().map {
            it.snr1000.toDouble() / 1000
        }

        private var sourceJob: Job? = null

        override fun open() {
            sourceJob = CoroutineScope(Dispatchers.Main).launch {
                flow.collect { value ->
                    addValue(graphSeries, min(max(SNR_MIN_VALUE, value), SNR_MAX_VALUE))
                }
            }
        }

        override fun close(reset: Boolean) {
            sourceJob?.cancel()
            sourceJob = null

            if (reset) {
                graphSeries.resetData(emptyArray())
            }
        }

        override fun getSeries() = listOf(graphSeries)
    }

    data class PhyPayload(
        val snr1000: Int
    )

    companion object {
        private const val SNR_MAX_VALUE = 30.0
        private const val SNR_MIN_VALUE = 0.0

        private val VISIBLE_PERIOD = TimeUnit.MINUTES.toMillis(5)
    }
}