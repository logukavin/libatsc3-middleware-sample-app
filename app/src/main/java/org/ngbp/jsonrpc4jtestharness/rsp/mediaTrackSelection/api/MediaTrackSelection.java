package org.ngbp.jsonrpc4jtestharness.rsp.mediaTrackSelection.api;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcService;


@JsonRpcService("")
public interface MediaTrackSelection {

    @JsonRpcMethod("org.atsc.track.selection")
    Object mediaTrackSelection();
}
