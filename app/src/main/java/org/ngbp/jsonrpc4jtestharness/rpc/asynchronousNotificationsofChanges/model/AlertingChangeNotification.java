package org.ngbp.jsonrpc4jtestharness.rpc.asynchronousNotificationsofChanges.model;

import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model.Alerting;

import java.util.List;

public class AlertingChangeNotification {
    public String msgType;
    private List<Alerting> alertList;
}
