package org.ngbp.jsonrpc4jtestharness.rpc.requestReceiverActions

import androidx.annotation.Nullable
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcError
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcParam
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType
import org.ngbp.jsonrpc4jtestharness.rpc.CustomException
import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse
import org.ngbp.jsonrpc4jtestharness.rpc.requestReceiverActions.model.AudioVolume

@JsonRpcType
interface IReceiverAction {
    @JsonRpcMethod("org.atsc.acquire.service")
    fun acquireService(): RpcResponse

    @JsonRpcMethod("org.atsc.scale-position")
    fun videoScalingAndPositioning(@JsonRpcParam("scaleFactor") scaleFactor: Double?, @JsonRpcParam("xPos") xPos: Double?, @JsonRpcParam("yPos") yPos: Double?): RpcResponse

    @JsonRpcError(value = CustomException::class, mode = JsonRpcError.Mode.THROW)
    @JsonRpcMethod(value = "org.atsc.setRMPURL")
    fun setRMPURL(@JsonRpcParam("operation") operation: String, @JsonRpcParam("rmpurl") rmpurl: String?, @JsonRpcParam(value = "rmpSyncTime", nullable = true) rmpSyncTime: Double?): RpcResponse

    @JsonRpcMethod("org.atsc.audioVolume")
    fun audioVolume(): AudioVolume
}