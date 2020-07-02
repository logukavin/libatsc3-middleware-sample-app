package org.ngbp.jsonrpc4jtestharness.rpc.filterCodes;

import org.ngbp.jsonrpc4jtestharness.rpc.filterCodes.model.GetFilterCodes;

import java.util.ArrayList;

public class FilterCodesImpl implements IFilterCodes {
    @Override
    public GetFilterCodes getFilterCodes() {
        GetFilterCodes.Filters filters = new GetFilterCodes.Filters();
        filters.expires = " 10";
        filters.filterCode = 10;
        ArrayList<GetFilterCodes.Filters> filtersList = new ArrayList<>();
        filtersList.add(filters);
        GetFilterCodes getFilterCodes = new GetFilterCodes();
        getFilterCodes.filters = filtersList;
        return getFilterCodes;
    }

    @Override
    public Object setFilterCodes() {
        return null;
    }
}
