package org.ngbp.jsonrpc4jtestharness;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcParam;
import com.googlecode.jsonrpc4j.JsonRpcService;

import java.util.List;

@JsonRpcService("")
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
