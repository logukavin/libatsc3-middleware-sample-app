package com.nextgenbroadcast.mobile.middleware

import com.nextgenbroadcast.mobile.core.model.PhyFrequency
import com.nextgenbroadcast.mobile.core.model.ReceiverState
import com.nextgenbroadcast.mobile.middleware.atsc3.source.IAtsc3Source

internal interface IAtsc3ServiceCore {
    fun openRoute(filePath: String): Boolean
    fun openRoute(source: IAtsc3Source): Boolean
    fun closeRoute()
    fun tune(frequency: PhyFrequency)

    fun getReceiverState(): ReceiverState
}