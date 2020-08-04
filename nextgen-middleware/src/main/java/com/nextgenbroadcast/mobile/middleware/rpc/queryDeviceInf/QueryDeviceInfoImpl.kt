package com.nextgenbroadcast.mobile.middleware.rpc.queryDeviceInf

import android.annotation.SuppressLint
import android.os.Build
import com.nextgenbroadcast.mobile.middleware.rpc.KeyCode
import com.nextgenbroadcast.mobile.middleware.rpc.queryDeviceInf.model.DeviceInfo
import com.nextgenbroadcast.mobile.middleware.rpc.queryDeviceInf.model.DeviceInput
import com.nextgenbroadcast.mobile.middleware.rpc.queryDeviceInf.model.Info
import java.text.SimpleDateFormat

class QueryDeviceInfoImpl() : IQueryDeviceInfo {
    @SuppressLint("SimpleDateFormat")
    override fun queryDeviceInfo(deviceInfoParams: List<String>?): DeviceInfo {
        return DeviceInfo()
                .apply {
                    deviceMake = Build.MANUFACTURER
                    deviceModel = Build.MODEL
                    deviceInput = DeviceInput().apply {
                        ArrowUp = KeyCode.ARROW_UP
                        ArrowDown = KeyCode.ARROW_DOWN
                        ArrowRight = KeyCode.ARROW_RIGHT
                        ArrowLeft = KeyCode.ARROW_LEFT
                        Select = KeyCode.SELECT
                        Back = KeyCode.BACK
                    }
                    deviceInfo = Info().apply {
                        numberOfTuners = 1
                        yearOfMfr = SimpleDateFormat("yyyy").format(Build.TIME).toInt()
                    }
                }
    }
}
