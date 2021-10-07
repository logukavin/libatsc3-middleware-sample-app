package com.nextgenbroadcast.mobile.middleware.scoreboard.view

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.jjoe64.graphview.series.BaseSeries
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.nextgenbroadcast.mobile.middleware.dev.chart.TemporalChartView
import com.nextgenbroadcast.mobile.middleware.scoreboard.R
import com.nextgenbroadcast.mobile.middleware.scoreboard.ScoreboardPagerActivity.Companion.SNR_MAX_VALUE
import com.nextgenbroadcast.mobile.middleware.scoreboard.ScoreboardPagerActivity.Companion.SNR_MIN_VALUE
import com.nextgenbroadcast.mobile.middleware.scoreboard.ScoreboardPagerActivity.Companion.normalizeValue
import com.nextgenbroadcast.mobile.middleware.scoreboard.entities.ChartConfiguration
import com.nextgenbroadcast.mobile.middleware.scoreboard.entities.ChartDataSource
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

    var isLegendVisible: Boolean
        get() {
            return legendRenderer.isVisible
        }
        set(value) {
            legendRenderer.isVisible = value
        }

    override fun onFinishInflate() {
        super.onFinishInflate()

        title = resources.getString(R.string.chart_phy_snr_title)
        val textColor = ContextCompat.getColor(context, R.color.white)

        titleColor = textColor
        with(gridLabelRenderer) {
            gridColor = ContextCompat.getColor(context, R.color.chart_grid_color)
            verticalLabelsColor = textColor
            horizontalLabelsColor = textColor
            verticalLabelsSecondScaleColor = textColor
            textSize = resources.getDimension(R.dimen.chart_label_text_size)
            numVerticalLabels = 4
        }

        setViewport(VISIBLE_PERIOD, 0.00, 100.00)
        beforeSourceSet = {
            setViewport(VISIBLE_PERIOD, it.getMinY(), it.getMaxY())
        }
    }

    class MultiDataSource(
        private val primarySources: List<ChartDataSource>,
        private val secondarySources: List<ChartDataSource>,
        private val primaryChartConfiguration: ChartConfiguration,
        private val secondaryChartConfiguration: ChartConfiguration
    ) : TemporalDataSource(VISIBLE_PERIOD) {

        private var sourceJob: Job? = null

        override fun open() {
            super.open()
            sourceJob = CoroutineScope(Dispatchers.Main).launch {
                addValueToChart(primarySources, primaryChartConfiguration) +
                    addValueToChart(secondarySources, secondaryChartConfiguration)
            }
        }

        private fun CoroutineScope.addValueToChart(
            sources: List<ChartDataSource>,
            chartConfiguration: ChartConfiguration
        ) = sources.map { dataSource ->
            launch {
                dataSource.data.collect { (time, value) ->
                    addValue(
                        dataSource.seriesConfig,
                        normalizeValue(value, chartConfiguration.minYValue, chartConfiguration.maxYValue),
                        time
                    )
                }
            }
        }

        override fun close(reset: Boolean) {
            super.close(reset)
            sourceJob?.cancel()
            sourceJob = null
            if (reset) {
                (primarySources + secondarySources).forEach { source ->
                    source.seriesConfig.resetData(emptyArray())
                }
            }
        }

        override fun getSeries(): List<BaseSeries<DataPoint>> = primarySources.map { source ->
            source.seriesConfig
        }

        override fun getSecondarySeries(): List<BaseSeries<DataPoint>> = secondarySources.map { source ->
            source.seriesConfig
        }

        override fun getSecondaryMinY() = secondaryChartConfiguration.minYValue
        override fun getSecondaryMaxY() = secondaryChartConfiguration.maxYValue
        override fun getMaxY(): Double = primaryChartConfiguration.maxYValue
        override fun getMinY(): Double = primaryChartConfiguration.minYValue

    }

    class DataSource(
        private val flow: Flow<TDataPoint>
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
        private val VISIBLE_PERIOD = TimeUnit.MINUTES.toMillis(5)
    }
}