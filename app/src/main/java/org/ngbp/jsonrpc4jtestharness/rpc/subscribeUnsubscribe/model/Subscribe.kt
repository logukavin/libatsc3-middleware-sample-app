package org.ngbp.jsonrpc4jtestharness.rpc.subscribeUnsubscribe.model

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Subscribe(
        var msgType: MutableList<String?>? = null
)