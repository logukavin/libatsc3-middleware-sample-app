package org.ngbp.jsonrpc4jtestharness.models;

public class JsonRpcResponse <T> {

    private Long id;
    private T result;
    private JsonRpcError error;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public T getResult() {
        return result;
    }

    public void setResult (T result) {
        this.result = result;
    }

    public JsonRpcError getError() {
        return error;
    }

    public void setError(JsonRpcError error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "\n{" +
                "\nid: " + id +
                "\n, result: " + result +
                "\n, error: " + error +
                '}';
    }
}
