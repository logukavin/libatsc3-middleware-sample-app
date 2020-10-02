package com.nextgenbroadcast.mobile.middleware.rpc.eventStream.model

data class SubscribeParams (
    var schemeIdUri: String? = null,
    var value: String? = null
)