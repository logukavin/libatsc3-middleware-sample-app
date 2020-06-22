package org.ngbp.jsonrpc4jtestharness.rsp.eventStream.api;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcService;

import org.ngbp.jsonrpc4jtestharness.models.JsonRpcResponse;


@JsonRpcService("")
public interface EventStream {

    @JsonRpcMethod("org.atsc.eventStream.subscribe")
    JsonRpcResponse<Object> eventStreamSubscribe ();

    @JsonRpcMethod("org.atsc.eventStream.unsubscribe")
    JsonRpcResponse<Object> eventStreamUnsubscribe ();

    @JsonRpcMethod("org.atsc.eventStream.unsubscribe")
    JsonRpcResponse<Object> eventStreamEvent ();
}
