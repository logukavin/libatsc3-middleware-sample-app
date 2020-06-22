package org.ngbp.jsonrpc4jtestharness.rsp.requestReceiverActions.api;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcService;

import org.ngbp.jsonrpc4jtestharness.models.JsonRpcResponse;
import org.ngbp.jsonrpc4jtestharness.rsp.requestReceiverActions.model.AudioVolume;

@JsonRpcService("")
public interface RequestReceiverActions {

    @JsonRpcMethod("org.atsc.acquire.service")
    JsonRpcResponse<Object> acquireService ();

    @JsonRpcMethod("org.atsc.scale-position")
    JsonRpcResponse<Object> videoScalingAndPositioning ();

    @JsonRpcMethod("org.atsc.service")
    JsonRpcResponse<Object> setRMPURL ();

    @JsonRpcMethod("org.atsc.audioVolume")
    JsonRpcResponse<AudioVolume> audioVolume ();
}
