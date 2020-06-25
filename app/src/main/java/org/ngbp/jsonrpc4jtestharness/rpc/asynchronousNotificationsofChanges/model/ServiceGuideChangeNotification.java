package org.ngbp.jsonrpc4jtestharness.rpc.asynchronousNotificationsofChanges.model;

import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model.ServiceGuideUrls;

import java.util.List;

public class ServiceGuideChangeNotification {
    public String msgType;
    public List<ServiceGuideUrls> urlList;
}
