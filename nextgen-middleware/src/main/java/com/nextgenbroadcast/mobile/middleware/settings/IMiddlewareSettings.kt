package com.nextgenbroadcast.mobile.middleware.settings

import android.location.Location

interface IMiddlewareSettings : IServerSettings, IClientSettings {
    val deviceId: String
    val advertisingId: String
    var location: Location?
    var frequencyList: List<Int>?
}