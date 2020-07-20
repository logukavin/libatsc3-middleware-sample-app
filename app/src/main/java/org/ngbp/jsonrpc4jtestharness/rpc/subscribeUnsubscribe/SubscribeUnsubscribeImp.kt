package org.ngbp.jsonrpc4jtestharness.rpc.subscribeUnsubscribe

import org.ngbp.jsonrpc4jtestharness.rpc.subscribeUnsubscribe.model.Subscribe

class SubscribeUnsubscribeImp : ISubscribeUnsubscribe {
    override fun integratedSubscribe(types: List<String>): Subscribe {
        return Subscribe(types)
    }

    override fun integratedUnsubscribe(types: List<String>): Subscribe {
        return Subscribe(types)
    }
}