package org.ngbp.jsonrpc4jtestharness.rsp.filterCodes.api;

import org.ngbp.jsonrpc4jtestharness.models.JsonRpcResponse;
import org.ngbp.jsonrpc4jtestharness.rsp.filterCodes.model.GetFilterCodes;

import java.util.ArrayList;

public class FilterCodesImpl implements FilterCodes {
    @Override
    public GetFilterCodes getFilterCodes() {
        GetFilterCodes.Filters filters =  new GetFilterCodes.Filters();
        filters.expires =" 10";
        filters.filterCode = 10;
        ArrayList<GetFilterCodes.Filters> filtersList = new ArrayList<>();
        filtersList.add(filters);
        GetFilterCodes getFilterCodes =  new GetFilterCodes();
        getFilterCodes.filters = filtersList;
        return getFilterCodes;
    }

    @Override
    public JsonRpcResponse<Object> setFilterCodes() {
        return null;
    }
}
