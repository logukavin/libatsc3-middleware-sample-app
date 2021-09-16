package com.nextgenbroadcast.mobile.middleware.scoreboard.entities

import org.ngbp.libatsc3.middleware.android.phy.models.RfPhyStatistics

data class PhyPayload (
    val stat: RfPhyStatistics,
    val timeStamp: Long
)