package org.ngbp.jsonrpc4jtestharness.jsonrpc2;

public class ComposedResponse {
    private Object result;
    public <T> T getResult(){
        return (T) result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
