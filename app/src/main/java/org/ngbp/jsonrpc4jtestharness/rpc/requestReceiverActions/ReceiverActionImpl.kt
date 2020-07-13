package org.ngbp.jsonrpc4jtestharness.rpc.requestReceiverActions

import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse
import org.ngbp.jsonrpc4jtestharness.rpc.manager.RPCManager
import org.ngbp.jsonrpc4jtestharness.rpc.requestReceiverActions.model.AudioVolume

class ReceiverActionImpl(private val rpcManager: RPCManager) : IReceiverAction {
    override fun acquireService(): RpcResponse {
        return RpcResponse()
    }

    override fun videoScalingAndPositioning(scaleFactor: Double?, xPos: Double?, yPos: Double?): RpcResponse {
        rpcManager.updateViewPosition(scaleFactor, xPos, yPos)
        return RpcResponse()
    }

    override fun setRMPURL(): RpcResponse {
        return RpcResponse()
    }

    override fun audioVolume(): AudioVolume {
        return AudioVolume()
    }
}