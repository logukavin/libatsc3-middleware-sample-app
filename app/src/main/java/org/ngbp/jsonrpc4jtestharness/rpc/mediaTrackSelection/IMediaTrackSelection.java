package org.ngbp.jsonrpc4jtestharness.rpc.mediaTrackSelection;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcService;


@JsonRpcService("")
public interface IMediaTrackSelection {

    @JsonRpcMethod("org.atsc.track.selection")
    Object mediaTrackSelection();
}
