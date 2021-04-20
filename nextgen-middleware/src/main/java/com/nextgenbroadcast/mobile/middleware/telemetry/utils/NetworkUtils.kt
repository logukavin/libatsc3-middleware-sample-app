package com.nextgenbroadcast.mobile.middleware.telemetry.utils

fun getIpv4FromInt(ipAddress: Int) =
        String.format("%d.%d.%d.%d", ipAddress and 0xff, ipAddress shr 8 and 0xff, ipAddress shr 16 and 0xff, ipAddress shr 24 and 0xff)