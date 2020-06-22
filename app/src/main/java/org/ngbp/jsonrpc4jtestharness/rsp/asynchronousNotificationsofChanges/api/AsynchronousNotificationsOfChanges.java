package org.ngbp.jsonrpc4jtestharness.rsp.asynchronousNotificationsofChanges.api;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcService;

import org.ngbp.jsonrpc4jtestharness.rsp.asynchronousNotificationsofChanges.model.NotifyParams;
import org.ngbp.jsonrpc4jtestharness.rsp.mapper.JsonRpcResponse;

@JsonRpcService("")
public interface AsynchronousNotificationsOfChanges {

    @JsonRpcMethod("org.atsc.notify")
    JsonRpcResponse<Object> contentAdvisoryRatingChangeNotification();

}
