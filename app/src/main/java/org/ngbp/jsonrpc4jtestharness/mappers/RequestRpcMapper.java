package org.ngbp.jsonrpc4jtestharness.mappers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.ngbp.jsonrpc4jtestharness.models.JsonRpcRequest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RequestRpcMapper <T> {

    public InputStream mapModelToInputStream(T input, String methodName) {
        JsonRpcRequest jsonRpcRequest = mapModelToSingleRpcRequest(input, methodName);

        Gson gson = new GsonBuilder().create();
        String jsonString = gson.toJson(jsonRpcRequest);
        return new ByteArrayInputStream(jsonString.getBytes());
    }

    public InputStream mapModelListToInputStream(List<T> inputs, String methodName) {
        List<JsonRpcRequest> requestList = mapModelsToBatchRpcRequest(inputs, methodName);

        Gson gson = new GsonBuilder().create();
        String jsonString = gson.toJson(requestList);
        return new ByteArrayInputStream(jsonString.getBytes());
    }

    public JsonRpcRequest<T> mapModelToSingleRpcRequest (T input, String methodName) {
        JsonRpcRequest jsonRpcRequest = new JsonRpcRequest<T>();
        jsonRpcRequest.setId(new Random().nextLong());
        jsonRpcRequest.setMethod(methodName);
        jsonRpcRequest.setParams(input);

        return jsonRpcRequest;
    }

    public List<JsonRpcRequest> mapModelsToBatchRpcRequest (List<T> inputs, String methodName) {
        List<JsonRpcRequest> requestList = new ArrayList<>();
        for (T input: inputs) {
            requestList.add(mapModelToSingleRpcRequest(input, methodName));
        }

        return requestList;
    }
}
