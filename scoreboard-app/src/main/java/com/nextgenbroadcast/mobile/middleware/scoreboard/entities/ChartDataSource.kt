package com.nextgenbroadcast.mobile.middleware.scoreboard.entities

import com.jjoe64.graphview.series.BaseSeries
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.coroutines.flow.Flow
import kotlin.math.max
import kotlin.math.min

data class ChartDataSource(
    val topic: String,
    val seriesConfig: BaseSeries<DataPoint>,
    val data: Flow<TDataPoint>
)
