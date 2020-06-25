package org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model;

import java.util.List;

public class Alerting {
    public String alertingType;
    public String alertingFragment;
    public String receiveTime;
    public List<FilteredEventList> filteredEventList;

    public class FilteredEventList{
        public String aeaId;
    }
}
