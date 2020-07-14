package org.ngbp.libatsc3.entities.service

import java.util.*
import kotlin.collections.ArrayList

data class Service(
        var serviceId: Int = 0,
        var globalServiceId: String? = null,
        var majorChannelNo: Int = 0,
        var minorChannelNo: Int = 0,
        var serviceCategory: Int = 0,
        var shortServiceName: String? = null,
        var broadcastSvcSignalingCollection: ArrayList<BroadcastSvcSignaling> = ArrayList()
) {
    override fun toString(): String {
        return String.format(Locale.US, "%d.%d %s", majorChannelNo, minorChannelNo, shortServiceName)
    }
}