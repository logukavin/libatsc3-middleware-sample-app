package org.ngbp.jsonrpc4jtestharness.rpc.asynchronousNotificationsofChanges

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType

@JsonRpcType("")
interface IAsynchronousNotificationsOfChanges {
    @JsonRpcMethod("org.atsc.notify")
    fun contentAdvisoryRatingChangeNotification(): Any?
}