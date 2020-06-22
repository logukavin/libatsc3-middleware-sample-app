package org.ngbp.jsonrpc4jtestharness.rsp.requestReceiverActions.api;

import org.ngbp.jsonrpc4jtestharness.rsp.mapper.JsonRpcResponse;
import org.ngbp.jsonrpc4jtestharness.rsp.requestReceiverActions.model.AudioVolume;

public class RequestReceiverActionsImpl implements RequestReceiverActions {
    @Override
    public JsonRpcResponse<Object> acquireService() {
        return null;
    }

    @Override
    public JsonRpcResponse<Object> videoScalingAndPositioning() {
        return null;
    }

    @Override
    public JsonRpcResponse<Object> setRMPURL() {
        return null;
    }

    @Override
    public JsonRpcResponse<AudioVolume> audioVolume() {
        return null;
    }
}
