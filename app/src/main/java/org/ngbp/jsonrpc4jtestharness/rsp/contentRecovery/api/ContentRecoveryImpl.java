package org.ngbp.jsonrpc4jtestharness.rsp.contentRecovery.api;

import org.ngbp.jsonrpc4jtestharness.rsp.contentRecovery.model.CecoveredComponentInfo;
import org.ngbp.jsonrpc4jtestharness.rsp.contentRecovery.model.ContentRecoveryState;
import org.ngbp.jsonrpc4jtestharness.rsp.mapper.JsonRpcResponse;

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
