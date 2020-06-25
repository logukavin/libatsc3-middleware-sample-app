package org.ngbp.jsonrpc4jtestharness.rpc.contentRecovery.model;

import java.util.List;

public class CecoveredComponentInfo {
    public List<Component> component;

    class Component {
        public String mediaType;
        public String componentID;
        public String descriptor;
    }
}
