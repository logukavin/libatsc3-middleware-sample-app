package org.ngbp.jsonrpc4jtestharness.jsonrpc2;

import java.util.List;

public interface ICallWrapper {
    public <T> T getResponse(String request);
    public List<ComposedResponse> getResponses(List<String> requests);
}
