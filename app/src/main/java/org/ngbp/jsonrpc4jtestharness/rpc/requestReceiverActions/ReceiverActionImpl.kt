package org.ngbp.jsonrpc4jtestharness.rpc.requestReceiverActions

import org.ngbp.jsonrpc4jtestharness.controller.IRPCController
import org.ngbp.jsonrpc4jtestharness.rpc.RpcErrorCode
import org.ngbp.jsonrpc4jtestharness.rpc.RpcException
import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse
import org.ngbp.jsonrpc4jtestharness.rpc.requestReceiverActions.model.AudioVolume
import org.ngbp.jsonrpc4jtestharness.utils.toMilliSec

class ReceiverActionImpl(
        private val rpcController: IRPCController
) : IReceiverAction {

    private val MIN_SYNCHRONIZATION_TIME = 5.0

    override fun acquireService(): RpcResponse {
        return RpcResponse()
    }

    override fun videoScalingAndPositioning(scaleFactor: Double?, xPos: Double?, yPos: Double?): RpcResponse {
        rpcController.updateRMPPosition(scaleFactor, xPos, yPos)
        return RpcResponse()
    }

    override fun setRMPURL(operation: String, rmpUrl: String?, rmpSyncTime: Double?): RpcResponse {
        when (operation) {
            "startRmp" -> {
                timeValidation(rmpSyncTime)
                rpcController.startMediaReceiver(rmpSyncTime.toMilliSec(), rmpUrl)
            }
            "stopRmp" -> {
                rpcController.stopMediaReceiver(rmpSyncTime.toMilliSec(), rmpUrl)
            }
            "resumeService" -> {
                timeValidation(rmpSyncTime)
                rpcController.startMediaReceiver(rmpSyncTime.toMilliSec(), rmpUrl)
            }
        }
        return RpcResponse()
    }

    private fun timeValidation(rmpSyncTime: Double?) {
        if (rmpSyncTime != null && rmpSyncTime <= MIN_SYNCHRONIZATION_TIME) {
            throw RpcException(RpcErrorCode.SYNCHRONIZATION_CANNOT_BE_ACHIEVED)
        }
    }

    override fun audioVolume(): AudioVolume {
        return AudioVolume()
    }

}

