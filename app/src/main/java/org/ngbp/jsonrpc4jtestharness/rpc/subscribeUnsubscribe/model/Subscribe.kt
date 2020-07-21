package org.ngbp.jsonrpc4jtestharness.rpc.subscribeUnsubscribe.model

import com.fasterxml.jackson.annotation.JsonInclude
import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Subscribe(
        var msgType: List<String>? = null
) : RpcResponse()