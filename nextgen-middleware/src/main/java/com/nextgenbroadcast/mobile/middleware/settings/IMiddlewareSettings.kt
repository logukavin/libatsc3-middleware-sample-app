package com.nextgenbroadcast.mobile.middleware.settings

interface IMiddlewareSettings : IServerSettings, IClientSettings {
    val deviceId: String
    val advertisingId: String
    var frequencyList: List<Int>?
}