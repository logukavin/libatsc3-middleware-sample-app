package org.ngbp.jsonrpc4jtestharness.rpc.subscribeUnsubscribe;


import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcService;

import org.ngbp.jsonrpc4jtestharness.rpc.subscribeUnsubscribe.model.Subscribe;

@JsonRpcService("")
public interface ISubscribeUnsubscribe {

    @JsonRpcMethod("org.atsc.subscribe")
    Subscribe integratedSubscribe();

    @JsonRpcMethod("org.atsc.unsubscribe")
    Subscribe integratedUnsubscribe();
}
