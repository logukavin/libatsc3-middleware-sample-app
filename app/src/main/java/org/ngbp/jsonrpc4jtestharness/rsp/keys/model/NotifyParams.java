package org.ngbp.jsonrpc4jtestharness.rsp.keys.model;

import java.util.List;

public class NotifyParams {
    public String msgType;
    public List<Timeout> timeout;
    public class Timeout{
        public String key;
        public Integer time;
    }
}
