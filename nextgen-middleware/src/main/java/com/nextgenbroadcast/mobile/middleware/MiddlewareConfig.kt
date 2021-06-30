package com.nextgenbroadcast.mobile.middleware

import com.nextgenbroadcast.mobile.core.isClass

object MiddlewareConfig {
    val DEV_TOOLS = isClass("com.nextgenbroadcast.mobile.middleware.dev.atsc3.PHYStatistics")
}