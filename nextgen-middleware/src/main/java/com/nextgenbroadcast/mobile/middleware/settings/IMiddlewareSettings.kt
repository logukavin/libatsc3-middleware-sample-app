package com.nextgenbroadcast.mobile.middleware.settings

interface IMiddlewareSettings : IServerSettings, IClientSettings, IReceiverSettings {
    val deviceId: String
    val advertisingId: String
}