package org.ngbp.jsonrpc4jtestharness.rsp.drm.model;

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
