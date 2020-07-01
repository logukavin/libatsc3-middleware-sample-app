package org.ngbp.jsonrpc4jtestharness.jsonrpc2;

import java.util.List;

public interface IRPCProcessor {
    public String processRequest(String request);
    public List<String>  processRequest(List<String> requests);
}
