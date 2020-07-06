package org.ngbp.jsonrpc4jtestharness.rpc.requestReceiverActions

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcParam
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType
import org.ngbp.jsonrpc4jtestharness.rpc.IBARpc
import org.ngbp.jsonrpc4jtestharness.rpc.requestReceiverActions.model.AudioVolume

@JsonRpcType
interface IReceiverAction {
    @JsonRpcMethod("org.atsc.acquire.service")
    fun acquireService(): Any?

    @JsonRpcMethod("org.atsc.scale-position")
    fun videoScalingAndPositioning(@JsonRpcParam("scaleFactor") scaleFactor: Double?, @JsonRpcParam("xPos") xPos: Double?, @JsonRpcParam("yPos") yPos: Double?): IBARpc?

    @JsonRpcMethod("org.atsc.service")
    fun setRMPURL(): Any?

    @JsonRpcMethod("org.atsc.audioVolume")
    fun audioVolume(): AudioVolume?
}