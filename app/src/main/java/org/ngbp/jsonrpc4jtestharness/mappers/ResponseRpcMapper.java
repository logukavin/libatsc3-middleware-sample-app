package org.ngbp.jsonrpc4jtestharness.mappers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import org.ngbp.jsonrpc4jtestharness.models.JsonRpcError;
import org.ngbp.jsonrpc4jtestharness.models.JsonRpcResponse;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ResponseRpcMapper<T> {

    public JsonRpcResponse mapSingleJsonRpcToResponseModel (String outputJsonResponse) {
        Gson gson = new GsonBuilder().create();
        JsonObject jsonObject = new Gson().fromJson(outputJsonResponse, JsonObject.class);

        Type type = new TypeToken<T>() {}.getType();

        return getJsonRpcResponse(type, gson, jsonObject);
    }


    public List<JsonRpcResponse> mapBatchJsonRpcToResponseModelList (String outputJsonResponse) {
        List<JsonRpcResponse> jsonRpcResponseList = new ArrayList<>();
        Type type = new TypeToken<T>() {}.getType();

        Gson gson = new GsonBuilder().create();
        JsonArray jsonArray = new Gson().fromJson(outputJsonResponse, JsonArray.class);

        for (JsonElement element : jsonArray) {
            JsonObject jsonObject = element.getAsJsonObject();

            jsonRpcResponseList.add((getJsonRpcResponse(type, gson, jsonObject)));
        }

        return jsonRpcResponseList;
    }

    private JsonRpcResponse getJsonRpcResponse(Type type, Gson gson, JsonObject jsonObject) {
        JsonRpcResponse jsonRpcResponse = new JsonRpcResponse<T>();
        jsonRpcResponse.setId(jsonObject.get("id").getAsLong());

        JsonElement jsonElementResult = jsonObject.get("result");

        if (jsonElementResult.isJsonNull()) {
            JsonObject jsonError = jsonObject.getAsJsonObject("error");
            jsonRpcResponse.setError(gson.fromJson(jsonError, JsonRpcError.class));
        } else {
            jsonRpcResponse.setResult(gson.fromJson(jsonElementResult, type));
        }

        return jsonRpcResponse;
    }
}
