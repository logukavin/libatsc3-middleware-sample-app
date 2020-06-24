package org.ngbp.jsonrpc4jtestharness.models;

public class JsonRpcError {
    int code;
    String message;

    @Override
    public String toString() {
        return "\n{" +
                "\ncode: " + code +
                "\n, message: '" + message + '\'' +
                '}';
    }
}
