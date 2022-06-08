package com.github.nmuzhichin.jsonrpc.context;

import com.github.nmuzhichin.jsonrpc.annotation.Constraint;
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcError;
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod;
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcParam;
import com.github.nmuzhichin.jsonrpc.internal.asserts.Assert;
import com.github.nmuzhichin.jsonrpc.internal.function.ExceptionSuppressor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

final class AnnotationLookup {

    @SuppressWarnings("ObjectAllocationInLoop")
    static Map<String, MethodMetadata> lookupMethodAnnotation(final Object object, final Class<?> objectType) {
        final List<Method> rpcMethods =
                MethodUtils.getMethodsListWithAnnotation(objectType, JsonRpcMethod.class, true, true);

        final Map<String, MethodMetadata> methods = new HashMap<>(rpcMethods.size());

        for (final Method m : rpcMethods) {
            final JsonRpcMethod rpcMethod = m.getDeclaredAnnotation(JsonRpcMethod.class);
            final JsonRpcError customError = m.getDeclaredAnnotation(JsonRpcError.class);

            final String methodName = StringUtils.defaultIfBlank(rpcMethod.value(), m.getName());
            final ExceptionSuppressor<MethodHandle> suppressorMH = () -> MethodHandles.publicLookup().unreflect(m).bindTo(object);
            final MethodHandle methodHandle = suppressorMH.suppress();
            final List<ParameterMetadata> parameterMetadata = lookupParameterAnnotation(m.getParameters());

            methods.put(methodName, new MethodMetadata(methodHandle,
                                                       parameterMetadata,
                                                       new CustomErrorMetadata(customError),
                                                       rpcMethod.cacheable(),
                                                       rpcMethod.strictArgsOrder()));
        }

        return methods;
    }

    @SuppressWarnings("ObjectAllocationInLoop")
    private static List<ParameterMetadata> lookupParameterAnnotation(final Parameter[] parameters) {
        final List<ParameterMetadata> arguments = new ArrayList<>(parameters.length);

        for (Parameter p : parameters) {
            final JsonRpcParam rpcParam = p.getDeclaredAnnotation(JsonRpcParam.class);
            Assert.requireNotNull(rpcParam, "@JsonRpcParam annotation must be present.");

            final Class<?> parameterClass = p.getType();
            final String parameterName = StringUtils.defaultIfBlank(rpcParam.value(), parameterClass.getName());
            final boolean parameterNullable = rpcParam.nullable();
            final Map<Constraint.Type, String> constraints =
                    parseParameterConstraints(p.getDeclaredAnnotation(Constraint.class), rpcParam.constraints());

            arguments.add(new ParameterMetadata(parameterName, parameterNullable, parameterClass, rpcParam.predefine(), constraints));
        }

        return arguments;
    }

    private static Map<Constraint.Type, String> parseParameterConstraints(final Constraint outer,
                                                                          final Constraint... constraints) {
        boolean outerNonNull = outer != null;
        final boolean innerNonNull = constraints != null && constraints.length > 0;

        final Map<Constraint.Type, String> constraintMap = new EnumMap<>(Constraint.Type.class);

        if (outerNonNull) {
            constraintMap.put(outer.type(), outer.value());
        }

        if (innerNonNull) {
            for (final Constraint constraint : constraints) {
                constraintMap.put(constraint.type(), constraint.value());
            }
        }

        return constraintMap;
    }
}
