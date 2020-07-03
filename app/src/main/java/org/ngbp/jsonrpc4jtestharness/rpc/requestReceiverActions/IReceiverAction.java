package org.ngbp.jsonrpc4jtestharness.rpc.requestReceiverActions;

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod;
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcParam;
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType;

import org.ngbp.jsonrpc4jtestharness.rpc.requestReceiverActions.model.AudioVolume;
import org.ngbp.jsonrpc4jtestharness.rpc.EmptyModel;

@JsonRpcType
public interface IReceiverAction {

    @JsonRpcMethod("org.atsc.acquire.service")
    Object acquireService();

    @JsonRpcMethod("org.atsc.scale-position")
    EmptyModel videoScalingAndPositioning(@JsonRpcParam("scaleFactor") Double scaleFactor, @JsonRpcParam("xPos") Double xPos, @JsonRpcParam("yPos") Double yPos);

    @JsonRpcMethod("org.atsc.service")
    Object setRMPURL();

    @JsonRpcMethod("org.atsc.audioVolume")
    AudioVolume audioVolume();
}
