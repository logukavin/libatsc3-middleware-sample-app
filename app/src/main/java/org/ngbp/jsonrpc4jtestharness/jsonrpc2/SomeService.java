package org.ngbp.jsonrpc4jtestharness.jsonrpc2;



import com.github.nmuzhichin.jsonrpc.annotation.Constraint;
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod;
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcParam;
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType;


import static com.github.nmuzhichin.jsonrpc.annotation.Constraint.Type.NOT_NULL;

@JsonRpcType("MockService")
public interface SomeService {
    @JsonRpcMethod
    void doSomething(@JsonRpcParam(value = "notify", constraints = @Constraint(type = NOT_NULL)) String value);

    @JsonRpcMethod("superAction")
    Model action(@JsonRpcParam("argumentStr") String v0,
                 @JsonRpcParam("argumentLong") Long v1,
                 @JsonRpcParam("myModel") AnotherModel model);
}
