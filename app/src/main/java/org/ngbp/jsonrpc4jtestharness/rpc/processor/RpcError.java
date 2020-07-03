package org.ngbp.jsonrpc4jtestharness.rpc.processor;

public class RpcError {
    private int code;
    private String message;

    public RpcError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
