package com.nextgenbroadcast.mobile.middleware.settings

import java.time.LocalDateTime

interface IMiddlewareSettings : IServerSettings, IClientSettings, IReceiverSettings {
    val deviceId: String
    val advertisingId: String
    var lastReportDate: LocalDateTime
}