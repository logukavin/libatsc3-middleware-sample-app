package org.ngbp.jsonrpc4jtestharness.rpc.processor;

import com.github.nmuzhichin.jsonrpc.model.response.errors.Error;

class InternalRpcError extends RpcError implements Error {

    InternalRpcError(int code, String message) {
        super(code, message);
    }

    @Override
    public int getCode() {
        return super.getCode();
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }

    @Override
    public Object getData() {
        return null;
    }
}
