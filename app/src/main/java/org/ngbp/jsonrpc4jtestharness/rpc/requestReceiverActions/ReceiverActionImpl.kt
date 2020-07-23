package org.ngbp.jsonrpc4jtestharness.rpc.requestReceiverActions

import org.ngbp.jsonrpc4jtestharness.controller.IRPCController
import org.ngbp.jsonrpc4jtestharness.rpc.RpcErrorCode
import org.ngbp.jsonrpc4jtestharness.rpc.RpcException
import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse
import org.ngbp.jsonrpc4jtestharness.rpc.requestReceiverActions.model.AudioVolume

class ReceiverActionImpl(
        private val rpcController: IRPCController
) : IReceiverAction {

    override fun acquireService(): RpcResponse {
        return RpcResponse()
    }

    override fun videoScalingAndPositioning(scaleFactor: Double, xPos: Double, yPos: Double): RpcResponse {
        rpcController.updateRMPPosition(scaleFactor, xPos, yPos)
        return RpcResponse()
    }

    override fun setRMPURL(operation: String, rmpUrl: String?, rmpSyncTime: Double?): RpcResponse {
        //TODO: currently we do not support delays
        if (rmpSyncTime != null) {
            throw RpcException(RpcErrorCode.SYNCHRONIZATION_CANNOT_BE_ACHIEVED)
        }

        when (operation) {
            "startRmp" -> {
                rpcController.requestMediaPlay(rmpUrl, convertSecToMilliSec(rmpSyncTime))
            }
            "stopRmp" -> {
                rpcController.requestMediaStop(delay = convertSecToMilliSec(rmpSyncTime))
            }
            "resumeService" -> {
                rpcController.requestMediaPlay(delay = convertSecToMilliSec(rmpSyncTime))
            }
        }

        return RpcResponse()
    }

    private fun convertSecToMilliSec(rmpSyncTime: Double?): Long {
        return rmpSyncTime?.let {
            if (rmpSyncTime == -1.0) {
                Long.MAX_VALUE
            } else {
                (rmpSyncTime * 1000).toLong()
            }
        } ?: 0
    }

    override fun audioVolume(): AudioVolume {
        return AudioVolume()
    }

}

