package org.ngbp.jsonrpc4jtestharness.rpc.requestReceiverActions

import org.ngbp.jsonrpc4jtestharness.rpc.RpcEmpty
import org.ngbp.jsonrpc4jtestharness.rpc.processor.RPCManager
import org.ngbp.jsonrpc4jtestharness.rpc.requestReceiverActions.model.AudioVolume

class ReceiverActionImpl(private val rpcManager: RPCManager) : IReceiverAction {
    override fun acquireService(): Any? {
        return null
    }

    override fun videoScalingAndPositioning(scaleFactor: Double?, xPos: Double?, yPos: Double?): RpcEmpty? {
        rpcManager.updateViewPosition(scaleFactor, xPos, yPos)
        return RpcEmpty()
    }

    override fun setRMPURL(): Any? {
        return null
    }

    override fun audioVolume(): AudioVolume? {
        return null
    }
}