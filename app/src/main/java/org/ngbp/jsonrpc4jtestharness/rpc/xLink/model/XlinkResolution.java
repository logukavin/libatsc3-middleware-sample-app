package org.ngbp.jsonrpc4jtestharness.rpc.xLink.model;

public class XlinkResolution {
    public String xlink;
    public Disposition disposition;
    public Timing timing;

    public class Timing{
        public Double currentPosition;
        public String periodStart;
        public Double duration;
    }
    public class Disposition {
        public Integer code;
        public String description;
    }
}
