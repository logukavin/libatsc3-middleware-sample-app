package org.ngbp.jsonrpc4jtestharness.rsp.asynchronousNotificationsofChanges.model;

import org.ngbp.jsonrpc4jtestharness.rsp.receiverQueryApi.model.Alerting;

import java.util.List;

public class AlertingChangeNotification {
    public String msgType;
    private List<Alerting> alertList;
}
