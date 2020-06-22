package org.ngbp.jsonrpc4jtestharness.rsp.subscribeUnsubscribe.api;


import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcService;

import org.ngbp.jsonrpc4jtestharness.models.JsonRpcResponse;
import org.ngbp.jsonrpc4jtestharness.rsp.subscribeUnsubscribe.model.Subscribe;

@JsonRpcService("")
public interface SubscribeUnsubscribe {

    @JsonRpcMethod("org.atsc.subscribe")
    JsonRpcResponse<Subscribe> integratedSubscribe();

    @JsonRpcMethod("org.atsc.unsubscribe")
    JsonRpcResponse<Subscribe> integratedUnsubscribe();
}
