package org.ngbp.jsonrpc4jtestharness.rpc.notification

import org.ngbp.jsonrpc4jtestharness.controller.IRPCController
import org.ngbp.jsonrpc4jtestharness.controller.model.PlaybackState
import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse
import org.ngbp.jsonrpc4jtestharness.rpc.contentRecovery.model.CecoveredComponentInfo

class RPCNotification(
        private val rpcController: IRPCController
): IRPCNotification {

    override fun rMPNotification(msgType: String, playbackState: Int): RpcResponse {
        return when (msgType) {
            "ratingChange" -> RpcResponse()
            "ratingBlock" -> RpcResponse()
            "serviceChange" -> RpcResponse()
            "captionState" -> RpcResponse()

            "langPref" -> RpcResponse()
            "captionDisplayPrefs" -> RpcResponse()
            "audioAccessibilityPref" -> RpcResponse()

            "MPDChange" -> RpcResponse()
            "alertingChange" -> RpcResponse()
            "contentChange" -> RpcResponse()
            "serviceGuideChange" -> RpcResponse()
            "contentRecoveryStateChange" -> CecoveredComponentInfo()
            "displayOverrideChange" -> RpcResponse()
            "recoveredComponentInfoChange" -> RpcResponse()

            "requestKeyTimeout" -> RpcResponse()

            "rmpMediaTimeChange" -> RpcResponse()
            "rmpPlaybackStateChange" -> {
                PlaybackState.valueOf(playbackState)?.let { state ->
                    rpcController.updateRMPState(state)
                }
                RpcResponse()
            }
            "rmpPlaybackRateChange" -> RpcResponse()

            "DRM" -> RpcResponse()
            "xlinkResolution" -> RpcResponse()

            else -> RpcResponse()
        }
    }

}