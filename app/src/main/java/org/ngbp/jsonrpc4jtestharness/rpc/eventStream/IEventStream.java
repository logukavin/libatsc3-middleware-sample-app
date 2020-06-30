package org.ngbp.jsonrpc4jtestharness.rpc.eventStream;

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod;
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType;


@JsonRpcType
public interface IEventStream {

    @JsonRpcMethod("org.atsc.eventStream.subscribe")
    Object eventStreamSubscribe();

    @JsonRpcMethod("org.atsc.eventStream.unsubscribe")
    Object eventStreamUnsubscribe();

    @JsonRpcMethod("org.atsc.eventStream.unsubscribe")
    Object eventStreamEvent();
}
