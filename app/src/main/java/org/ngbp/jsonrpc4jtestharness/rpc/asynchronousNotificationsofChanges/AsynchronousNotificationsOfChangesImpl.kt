package org.ngbp.jsonrpc4jtestharness.rpc.asynchronousNotificationsofChanges

import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse

class AsynchronousNotificationsOfChangesImpl : IAsynchronousNotificationsOfChanges {
    override fun contentAdvisoryRatingChangeNotification(): RpcResponse {
        return RpcResponse()
    }
}