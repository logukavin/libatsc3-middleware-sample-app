package com.nextgenbroadcast.mobile.middleware.rpc.subscribeUnsubscribe.model

data class LaunchParams(
    val launch: LaunchParamsItem
)

data class LaunchParamsItem(
    val launchUrl: String,
    val appType: String
)