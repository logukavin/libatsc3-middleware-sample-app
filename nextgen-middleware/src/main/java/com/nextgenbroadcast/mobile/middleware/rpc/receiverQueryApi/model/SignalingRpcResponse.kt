package com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.model

data class SignalingRpcResponse(
    var objectList: List<SignalingInfo>
) {
    data class SignalingInfo(
        var name: String,
        var group: String?,
        var version: String,
        var table: String
    )
}
