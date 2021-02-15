package com.nextgenbroadcast.mobile.middleware.analytics.model

internal class CDMObject(
        val deviceInfo: DeviceInfo,
        val avService: List<AVService>
) {
    val protocolVersion: String = "0x00"
}