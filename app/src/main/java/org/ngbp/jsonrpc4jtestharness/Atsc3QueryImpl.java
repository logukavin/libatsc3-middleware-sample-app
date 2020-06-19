package org.ngbp.jsonrpc4jtestharness;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class Atsc3QueryImpl implements IAtsc3Query {

    @Override
    public ServiceResponse queryService() {
        Log.i("queryService", String.format("org.atsc.query.service with no arguments"));
        ServiceResponse serviceResponse = new ServiceResponse();
        serviceResponse.service = "tag:sinclairplatform.com,2020:KSNV:2089";

        return serviceResponse;
    }

    @Override
    public List<String> queryService2(List<String> properties) {
        Log.i("queryService2", String.format("org.atsc.query.service with properties: %s", properties));
        ArrayList<String> result = new ArrayList<String>();
        result.add("ngbp.org1");
        result.add("ngbp.org2");
        result.add("ngbp.org3");
        return result;
    }
}
