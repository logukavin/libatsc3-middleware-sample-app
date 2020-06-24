package org.ngbp.jsonrpc4jtestharness.rsp.filterCodes.model;

import java.util.List;

public class GetFilterCodes {
    public List<Filters> filters;
    public static class Filters{
        public Integer filterCode;
        public String expires;

    }
}
