package org.ngbp.jsonrpc4jtestharness.rpc.subscribeUnsubscribe.model

import org.ngbp.jsonrpc4jtestharness.rpc.EmptyModel

data class Subscribe(
        var msgType: MutableList<String?>? = null
) : EmptyModel()