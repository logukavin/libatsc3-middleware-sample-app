package org.ngbp.jsonrpc4jtestharness.rpc.requestReceiverActions;

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod;
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType;

import org.ngbp.jsonrpc4jtestharness.rpc.requestReceiverActions.model.AudioVolume;

@JsonRpcType
public interface IRequestReceiverActions {

    @JsonRpcMethod("org.atsc.acquire.service")
    Object acquireService();

    @JsonRpcMethod("org.atsc.scale-position")
    Object videoScalingAndPositioning();

    @JsonRpcMethod("org.atsc.service")
    Object setRMPURL();

    @JsonRpcMethod("org.atsc.audioVolume")
    AudioVolume audioVolume();
}
