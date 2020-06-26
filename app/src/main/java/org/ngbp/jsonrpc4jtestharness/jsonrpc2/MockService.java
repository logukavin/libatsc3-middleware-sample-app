package org.ngbp.jsonrpc4jtestharness.jsonrpc2;

import com.github.nmuzhichin.jsonrpc.annotation.Constraint;
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcError;
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod;
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcParam;
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType;

import java.util.List;

@JsonRpcType("MockService")
public interface MockService {
    @JsonRpcMethod("listOf")
    List<String> of(@JsonRpcParam("stringSingle") final String single,
                    @JsonRpcParam("varargs") final Integer... varargs);

    @JsonRpcMethod
    String self(@JsonRpcParam("single") final String single);

    @JsonRpcMethod(cacheable = true)
    void invoice(@JsonRpcParam("model") final MockModel model);

    @JsonRpcMethod(cacheable = true)
    String voice(@JsonRpcParam("model") final MockModel model);

    @JsonRpcMethod
    String randomize();

    @JsonRpcMethod
    long randomizeV2(@JsonRpcParam(predefine = true) byte[] seed);

    @JsonRpcError(Exception.class)
    @JsonRpcMethod
    String randomizeV3(@JsonRpcParam("id") @Constraint(value = "1", type = Constraint.Type.MIN) Long id);

    @JsonRpcMethod(strictArgsOrder = true)
    String strictCall(@JsonRpcParam("name") String name, @JsonRpcParam("age") Long age);

    @JsonRpcError(Exception.class)
    @JsonRpcMethod(strictArgsOrder = true)
    Integer withCustomError();

    @JsonRpcError(Exception.class)
    @JsonRpcMethod(strictArgsOrder = true)
    Integer withCustomErrorV2();

    @JsonRpcError(value = Exception.class, mode = JsonRpcError.Mode.THROW)
    @JsonRpcMethod(strictArgsOrder = true)
    Integer withCustomErrorThrow();
}
