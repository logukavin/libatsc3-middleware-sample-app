package org.ngbp.jsonrpc4jtestharness.rsp.contentRecovery.api;

import org.ngbp.jsonrpc4jtestharness.models.JsonRpcResponse;
import org.ngbp.jsonrpc4jtestharness.rsp.contentRecovery.model.CecoveredComponentInfo;
import org.ngbp.jsonrpc4jtestharness.rsp.contentRecovery.model.ContentRecoveryState;

public class ContentRecoveryImpl implements ContentRecovery {
    @Override
    public JsonRpcResponse<ContentRecoveryState> queryContentRecoveryState() {
        return null;
    }

    @Override
    public JsonRpcResponse<CecoveredComponentInfo> queryRecoveredComponentInfo() {
        return null;
    }

    @Override
    public JsonRpcResponse<CecoveredComponentInfo> contentRecoveryStateChangeNotification() {
        return null;
    }
}
