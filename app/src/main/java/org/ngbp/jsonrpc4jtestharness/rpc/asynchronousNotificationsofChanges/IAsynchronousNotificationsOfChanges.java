package org.ngbp.jsonrpc4jtestharness.rpc.asynchronousNotificationsofChanges;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcService;

@JsonRpcService("")
public interface IAsynchronousNotificationsOfChanges {

    @JsonRpcMethod("org.atsc.notify")
    Object contentAdvisoryRatingChangeNotification();

}
