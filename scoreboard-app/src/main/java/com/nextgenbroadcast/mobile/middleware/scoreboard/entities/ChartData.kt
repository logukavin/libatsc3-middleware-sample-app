package com.nextgenbroadcast.mobile.middleware.scoreboard.entities

data class ChartData(
    val primaryChartConfiguration: ChartConfiguration = ChartConfiguration(0.00, 100.0),
    val secondaryChartConfiguration: ChartConfiguration = ChartConfiguration(0.00, 100.0),
    val primaryDataSources: List<ChartDataSource>,
    val secondaryDataSources: List<ChartDataSource> = emptyList()
)