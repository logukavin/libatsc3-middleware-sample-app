package org.ngbp.jsonrpc4jtestharness.rsp.eventStream.api;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcService;


@JsonRpcService("")
public interface EventStream {

    @JsonRpcMethod("org.atsc.eventStream.subscribe")
    Object eventStreamSubscribe ();

    @JsonRpcMethod("org.atsc.eventStream.unsubscribe")
    Object eventStreamUnsubscribe ();

    @JsonRpcMethod("org.atsc.eventStream.unsubscribe")
    Object eventStreamEvent ();
}
