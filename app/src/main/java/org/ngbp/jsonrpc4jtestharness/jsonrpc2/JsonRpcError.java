package org.ngbp.jsonrpc4jtestharness.jsonrpc2;

import com.github.nmuzhichin.jsonrpc.model.response.errors.Error;

public class JsonRpcError implements Error {
    private int code;
    private String message;

    public JsonRpcError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public Object getData() {
        return null;
    }
}
