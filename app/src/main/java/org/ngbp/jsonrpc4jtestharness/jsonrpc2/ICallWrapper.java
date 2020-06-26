package org.ngbp.jsonrpc4jtestharness.jsonrpc2;

import java.util.List;

public interface ICallWrapper {
    public <T> T getResponse(RequestParams requestParams);
    public List<ComposedResponse> getResponses(List<RequestParams>  method);
}
