package com.nextgenbroadcast.mobile.middleware.analytics

import android.location.Location

class DeviceInfo(
        val location: Location?,
        val clockSource: Int
) {
    val deviceID: String = android.os.Build.ID // TODO: It's not a device ID!!!
    val deviceModel: String = android.os.Build.MODEL
    val deviceManufacturer: String = android.os.Build.MANUFACTURER
    val deviceOS: String = "Android"
    val peripheralDevice: String = "FALSE"
}