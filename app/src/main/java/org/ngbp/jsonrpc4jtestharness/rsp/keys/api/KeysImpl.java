package org.ngbp.jsonrpc4jtestharness.rsp.keys.api;

import org.ngbp.jsonrpc4jtestharness.models.JsonRpcResponse;
import org.ngbp.jsonrpc4jtestharness.rsp.filterCodes.model.GetFilterCodes;

public class KeysImpl implements Keys {
    @Override
    public JsonRpcResponse<GetFilterCodes> requestKeys() {
        return null;
    }

    @Override
    public JsonRpcResponse<Object> relinquishKeys() {
        return null;
    }

    @Override
    public JsonRpcResponse<Object> requestKeysTimeout() {
        return null;
    }
}
