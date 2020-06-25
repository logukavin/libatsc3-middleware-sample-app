package org.ngbp.jsonrpc4jtestharness.rpc.drm.model;

import java.util.List;

public class DRMOperationParam {
    public String systemId;
    public String service;
    public List<Message> message;
    class Message{
        public String operation;
        public String licenseUri;
    }
}
