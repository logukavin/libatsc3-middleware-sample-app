package org.ngbp.jsonrpc4jtestharness.models;

public class JsonRpcError {
    private int code;
    private String message;
    private Long id;
    public static String NULL_POINTER_ERROR = "Internal NullPointerException error";
    public static String PARSE_ERROR = "Parse error";
    public static int NULL_POINTER_CODE = -32603;
    public static int PARSE_ERROR_CODE = -32700;

    public JsonRpcError(int code, String message, Long requestId) {
        this.code = code;
        this.message = message;
        this.id = requestId;
    }

    @Override
    public String toString() {
        return "{\"jsonrpc\":\"2.0\",\"error\":{\"code\": " + code + ", \"message\": \"" + message + "\"},\"id\":\"" + id + "\"}";
    }
}
