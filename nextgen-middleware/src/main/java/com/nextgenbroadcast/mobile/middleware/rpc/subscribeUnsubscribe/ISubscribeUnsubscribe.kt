package com.nextgenbroadcast.mobile.middleware.rpc.subscribeUnsubscribe

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcParam
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType
import com.nextgenbroadcast.mobile.middleware.rpc.subscribeUnsubscribe.model.LaunchParams
import com.nextgenbroadcast.mobile.middleware.rpc.subscribeUnsubscribe.model.SubscribeRpcResponse

@JsonRpcType
interface ISubscribeUnsubscribe {
    @JsonRpcMethod("org.atsc.subscribe")
    fun integratedSubscribe(
        @JsonRpcParam("msgType") msgType: List<String>,
        @JsonRpcParam("launchParams", nullable = true) launchParams: List<LaunchParams>?
    ): SubscribeRpcResponse

    @JsonRpcMethod("org.atsc.unsubscribe")
    fun integratedUnsubscribe(@JsonRpcParam("msgType") msgType: List<String>): SubscribeRpcResponse
}