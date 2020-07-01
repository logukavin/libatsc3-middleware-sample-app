package org.ngbp.jsonrpc4jtestharness.rpc.requestReceiverActions;

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod;
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcParam;
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType;

import org.ngbp.jsonrpc4jtestharness.rpc.requestReceiverActions.model.AudioVolume;
import org.ngbp.jsonrpc4jtestharness.rpc.requestReceiverActions.model.VideoScalingPositioningParams;

import java.util.List;

@JsonRpcType
public interface IRequestReceiverActions {

    @JsonRpcMethod("org.atsc.acquire.service")
    Object acquireService();

    @JsonRpcMethod("org.atsc.scale-position")
    Object videoScalingAndPositioning(@JsonRpcParam("scaleFactor") Double scaleFactor, @JsonRpcParam("xPos") Double xPos, @JsonRpcParam("yPos") Double yPos);

    @JsonRpcMethod("org.atsc.service")
    Object setRMPURL();

    @JsonRpcMethod("org.atsc.audioVolume")
    AudioVolume audioVolume();
}
