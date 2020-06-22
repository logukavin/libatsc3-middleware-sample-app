package org.ngbp.jsonrpc4jtestharness.rsp.asynchronousNotificationsofChanges.api;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcService;

import org.ngbp.jsonrpc4jtestharness.models.JsonRpcResponse;
import org.ngbp.jsonrpc4jtestharness.rsp.asynchronousNotificationsofChanges.model.NotifyParams;

@JsonRpcService("")
public interface AsynchronousNotificationsOfChanges {

    @JsonRpcMethod("org.atsc.notify")
    Object contentAdvisoryRatingChangeNotification();

}
