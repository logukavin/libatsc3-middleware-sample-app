package com.nextgenbroadcast.mobile.middleware.analytics

class CDMObject(
        val deviceInfo: DeviceInfo,
        val avService: List<AVService>
) {
    val protocolVersion: String = "0x00"
}