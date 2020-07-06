package org.ngbp.jsonrpc4jtestharness.rpc.mediaTrackSelection

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType

@JsonRpcType
interface IMediaTrackSelection {
    @JsonRpcMethod("org.atsc.track.selection")
    fun mediaTrackSelection(): Any?
}