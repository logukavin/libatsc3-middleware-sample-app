package com.nextgenbroadcast.mobile.middleware.sample.chart

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.jjoe64.graphview.DefaultLabelFormatter
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.BaseSeries
import com.jjoe64.graphview.series.DataPoint
import com.nextgenbroadcast.mobile.middleware.sample.R
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

open class TemporalChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : GraphView(context, attrs, defStyleAttr) {

    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
    private val timeZone = ZoneId.systemDefault()

    private var source: TemporalDataSource? = null

    override fun onFinishInflate() {
        super.onFinishInflate()

        val textColor = ContextCompat.getColor(context, R.color.white)

        titleColor = textColor
        with(gridLabelRenderer) {
            gridColor = ContextCompat.getColor(context, R.color.chart_grid_color)
            verticalLabelsColor = textColor
            horizontalLabelsColor = textColor
            labelFormatter = object : DefaultLabelFormatter() {
                override fun formatLabel(value: Double, isValueX: Boolean): String? {
                    return if (!isValueX) {
                        super.formatLabel(value, isValueX)
                    } else if ((source?.getTotalCount() ?: 0) > 2) {
                        LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(value.toLong()),
                            timeZone
                        ).format(timeFormat)
                    } else null
                }
            }
        }
    }

    fun setDataSource(source: TemporalDataSource?) {
        this.source?.close()

        this.source = source

        removeAllSeries()

        if (source == null) return

        source.getSeries().forEach {
            addSeries(it)
        }

        if (isAttachedToWindow) {
            source.open()
        }
    }

    fun setViewport(visiblePeriodMills: Long, minValue: Double, maxValue: Double) {
        with(viewport) {
            isXAxisBoundsManual = true
            setMinX(0.0)
            setMaxX(visiblePeriodMills.toDouble().toSeconds())
            isYAxisBoundsManual = true
            setMinY(minValue)
            setMaxY(maxValue)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        source?.open()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        source?.close()
    }

    abstract class TemporalDataSource(
        private val visiblePeriod: Long
    ) : IChartDataSource {
        private var eventCounter = 0
        private var eventStartTime: Long = 0
        private var currentSpeed: Long = 0
        private var totalValueCount: Long = 0

        fun addValue(graphSeries: BaseSeries<DataPoint>, value: Double) {
            val currentTime = System.currentTimeMillis()
            if (eventCounter == 0) {
                eventStartTime = currentTime
            }

            if (eventCounter > SPEED_UPDATE_ITEM_COUNT) {
                currentSpeed = ((currentTime - eventStartTime) / SPEED_UPDATE_ITEM_COUNT)
                eventCounter = 0
                eventStartTime = currentTime
            }

            val dataPoint = DataPoint(currentTime.toDouble().toSeconds(), value)

            val maxValueCount = if (currentSpeed == 0L) {
                INITIAL_VALUE_COUNT
            } else {
                INITIAL_VALUE_COUNT + (visiblePeriod / currentSpeed)
            }

            graphSeries.appendData(dataPoint, true, maxValueCount.toInt())

            eventCounter++
            totalValueCount++
        }

        fun getTotalCount() = totalValueCount

        companion object {
            private const val SPEED_UPDATE_ITEM_COUNT = 10
            private const val INITIAL_VALUE_COUNT = 100
        }
    }
}

private fun Double.toSeconds() = this / 1000