package com.nextgenbroadcast.mobile.middleware.settings

interface IServerSettings {
    val hostName: String
    val httpPort: Int
    val httpsPort: Int
    val wsPort: Int
    val wssPort: Int
}