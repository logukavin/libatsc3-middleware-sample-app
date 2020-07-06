package org.ngbp.jsonrpc4jtestharness.rpc.subscribeUnsubscribe

import org.ngbp.jsonrpc4jtestharness.rpc.EmptyModel
import org.ngbp.jsonrpc4jtestharness.rpc.subscribeUnsubscribe.model.Subscribe

class SubscribeUnsubscribeImp : ISubscribeUnsubscribe {
    override fun integratedSubscribe(): EmptyModel {
        return EmptyModel()
    }

    override fun integratedUnsubscribe(): Subscribe? {
        return null
    }
}