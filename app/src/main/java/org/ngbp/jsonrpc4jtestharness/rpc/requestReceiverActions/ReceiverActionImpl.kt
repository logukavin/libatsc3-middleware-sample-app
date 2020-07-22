package org.ngbp.jsonrpc4jtestharness.rpc.requestReceiverActions

import org.ngbp.jsonrpc4jtestharness.controller.IRPCController
import org.ngbp.jsonrpc4jtestharness.controller.model.PlaybackState
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

    override fun videoScalingAndPositioning(scaleFactor: Double?, xPos: Double?, yPos: Double?): RpcResponse {
        rpcController.updateRMPPosition(scaleFactor, xPos, yPos)
        return RpcResponse()
    }

    override fun setRMPURL(operation: String, rmpurl: String?, rmpSyncTime: Double?): RpcResponse {
        when (operation) {
            "startRmp" -> {
                if (rmpSyncTime == null) {
                    setUpRMPData(rmpurl, rmpSyncTime, PlaybackState.PLAYING)
                } else {
                    if (rpcController.playbackState == PlaybackState.PLAYING) {
                        setUpRMPData(rmpurl, rmpSyncTime, PlaybackState.PLAYING)
                    } else if (rpcController.playbackState == PlaybackState.PLAYING && (rpcController.rmpUrl.equals(rmpurl) && rmpSyncTime.equals(-1.0))) {
                        setUpRMPData(rmpurl, rmpSyncTime, PlaybackState.PLAYING)
                    }
                }
                rpcController.rmpOperation = null
            }
            "stopRmp" -> {
                if (rpcController.playbackState == PlaybackState.PLAYING && rmpSyncTime == null) {
                    setUpRMPData(rmpurl, rmpSyncTime, PlaybackState.PAUSED)
                    rpcController.rmpOperation = operation
                } else {
                    if (rpcController.playbackState == PlaybackState.PLAYING && rmpSyncTime != null) {
                        setUpRMPData(rmpurl, rmpSyncTime, PlaybackState.PLAYING)
                        rpcController.rmpOperation = operation

                    }
                }
            }
            "resumeService" -> {
                if ((rpcController.playbackState == PlaybackState.PLAYING && (rpcController.rmpUrl == rpcController.currentMPD)) ||
                        rpcController.rmpOperation == "stopRmp" && rmpSyncTime == null) {
                    setUpRMPData(null, null, PlaybackState.PLAYING)
                }
                if ((rpcController.playbackState == PlaybackState.PLAYING && (rpcController.rmpUrl == rpcController.currentMPD)) && rmpSyncTime != null) {
                    setUpRMPData(null, rmpSyncTime, PlaybackState.PLAYING)
                }
                rpcController.rmpOperation = null
            }
        }
        return RpcResponse()
    }

    private fun setUpRMPData(rmpurl: String?, rmpSyncTime: Double?, state: PlaybackState) {
        if (rmpSyncTime != null && rmpSyncTime <= 5.0) {
            throw RpcException(RpcErrorCode.SYNCHRONIZATION_CANNOT_BE_ACHIEVED)
        } else {
            rpcController.rmpUrl = rmpurl
            rpcController.rmpSyncTime = rmpSyncTime
            rpcController.updateRMPState(state)
        }

    }

    override fun audioVolume(): AudioVolume {
        return AudioVolume()
    }

}

