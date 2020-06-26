package org.ngbp.jsonrpc4jtestharness.rpc.mediaTrackSelection;

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod;
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType;


@JsonRpcType
public interface IMediaTrackSelection {

    @JsonRpcMethod("org.atsc.track.selection")
    Object mediaTrackSelection();
}
