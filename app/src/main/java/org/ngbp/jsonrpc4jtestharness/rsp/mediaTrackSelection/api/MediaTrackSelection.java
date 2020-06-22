package org.ngbp.jsonrpc4jtestharness.rsp.mediaTrackSelection.api;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcService;

import org.ngbp.jsonrpc4jtestharness.models.JsonRpcResponse;


@JsonRpcService("")
public interface MediaTrackSelection {

    @JsonRpcMethod("org.atsc.track.selection")
    JsonRpcResponse<Object> mediaTrackSelection();
}
