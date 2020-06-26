package org.ngbp.jsonrpc4jtestharness;

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod;
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcParam;
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType;

import java.util.List;

@JsonRpcType
public interface IAtsc3Query {
    @JsonRpcMethod("org.atsc.query.service")
    ServiceResponse queryService();

    //sample usage of inner class domain model for POC
    //todo: use @Data and @Builder from lombok
    public static class ServiceResponse {
        public String service;
    }






    @JsonRpcMethod("org.atsc.query.service2")
    List<String> queryService2(@JsonRpcParam(value="properties") final List<String> properties);


}
