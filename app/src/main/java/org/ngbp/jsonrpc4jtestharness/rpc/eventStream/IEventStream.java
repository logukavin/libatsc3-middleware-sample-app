package org.ngbp.jsonrpc4jtestharness.rpc.eventStream;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcService;


@JsonRpcService("")
public interface IEventStream {

    @JsonRpcMethod("org.atsc.eventStream.subscribe")
    Object eventStreamSubscribe();

    @JsonRpcMethod("org.atsc.eventStream.unsubscribe")
    Object eventStreamUnsubscribe();

    @JsonRpcMethod("org.atsc.eventStream.unsubscribe")
    Object eventStreamEvent();
}
