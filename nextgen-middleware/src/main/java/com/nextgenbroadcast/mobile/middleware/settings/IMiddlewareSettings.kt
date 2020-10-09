package com.nextgenbroadcast.mobile.middleware.settings

import com.nextgenbroadcast.mobile.middleware.location.FrequencyLocation

interface IMiddlewareSettings : IServerSettings, IClientSettings, IReceiverSettings {
    val deviceId: String
    val advertisingId: String
    var frequencyLocation: FrequencyLocation?
}