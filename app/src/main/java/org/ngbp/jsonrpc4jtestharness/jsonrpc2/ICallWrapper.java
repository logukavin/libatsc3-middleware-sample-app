package org.ngbp.jsonrpc4jtestharness.jsonrpc2;

import java.util.List;

public interface ICallWrapper {
    public <T> T processRequest(String request);
    public List<ComposedResponse> processRequest(List<String> requests);
}
