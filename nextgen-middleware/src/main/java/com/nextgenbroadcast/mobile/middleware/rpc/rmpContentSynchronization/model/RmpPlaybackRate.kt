package com.nextgenbroadcast.mobile.middleware.rpc.rmpContentSynchronization.model

import com.nextgenbroadcast.mobile.middleware.rpc.RpcResponse

data class RmpPlaybackRate(
    val playbackRate: Float
) : RpcResponse()