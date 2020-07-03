package org.ngbp.jsonrpc4jtestharness.rpc.asynchronousNotificationsofChanges.model

import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model.Alerting

data class AlertingChangeNotification (
    var msgType: String? = null,
    var alertList: MutableList<Alerting?>? = null
)