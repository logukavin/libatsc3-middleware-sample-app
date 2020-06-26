package org.ngbp.jsonrpc4jtestharness.rpc.subscribeUnsubscribe;


import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod;
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType;

import org.ngbp.jsonrpc4jtestharness.rpc.subscribeUnsubscribe.model.Subscribe;

@JsonRpcType
public interface ISubscribeUnsubscribe {

    @JsonRpcMethod("org.atsc.subscribe")
    Subscribe integratedSubscribe();

    @JsonRpcMethod("org.atsc.unsubscribe")
    Subscribe integratedUnsubscribe();
}
