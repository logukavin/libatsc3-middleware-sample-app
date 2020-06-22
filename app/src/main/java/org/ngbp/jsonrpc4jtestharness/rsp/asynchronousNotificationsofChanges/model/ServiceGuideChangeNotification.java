package org.ngbp.jsonrpc4jtestharness.rsp.asynchronousNotificationsofChanges.model;

import org.ngbp.jsonrpc4jtestharness.rsp.receiverQueryApi.model.ServiceGuideUrls;

import java.util.List;

public class ServiceGuideChangeNotification {
    public String msgType;
    public List<ServiceGuideUrls> urlList;
}
