package org.ngbp.jsonrpc4jtestharness.jsonrpc2;

public class ComposedResponse<T> {
    private T result;
    public <T> T getResult(){
        return (T) result;
    }

    public void setResult(T result) {
        this.result = result;
    }
}
