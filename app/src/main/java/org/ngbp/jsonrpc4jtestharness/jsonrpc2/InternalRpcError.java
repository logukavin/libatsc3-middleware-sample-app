package org.ngbp.jsonrpc4jtestharness.jsonrpc2;

class InternalRpcError extends JsonRpcError {
    static int PARSING_ERROR_CODE = -1;

    InternalRpcError(int code, String message) {
        super(code, message);
    }
}
