package org.ngbp.jsonrpc4jtestharness.rsp.requestReceiverActions.api;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcService;

import org.ngbp.jsonrpc4jtestharness.rsp.requestReceiverActions.model.AudioVolume;

@JsonRpcService("")
public interface RequestReceiverActions {

    @JsonRpcMethod("org.atsc.acquire.service")
    Object acquireService ();

    @JsonRpcMethod("org.atsc.scale-position")
    Object videoScalingAndPositioning ();

    @JsonRpcMethod("org.atsc.service")
    Object setRMPURL ();

    @JsonRpcMethod("org.atsc.audioVolume")
    AudioVolume audioVolume ();
}
