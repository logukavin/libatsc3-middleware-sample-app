package com.github.nmuzhichin.jsonrpc.context;

import javax.annotation.Nonnull;
import java.lang.invoke.MethodHandle;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class MethodMetadata implements Metadata {
    private final MethodHandle methodHandle;
    private final List<ParameterMetadata> arguments;
    private final CustomErrorMetadata customErrorMetadata;
    private final Class<?> returnType;
    private final boolean cacheable;
    private final boolean strictArgsOrder;

    MethodMetadata(@Nonnull final MethodHandle methodHandle,
                   @Nonnull final List<ParameterMetadata> orderedParameterMetadata,
                   @Nonnull final CustomErrorMetadata customErrorMetadata,
                   boolean cacheable,
                   boolean strictArgsOrder) {

        this.methodHandle = methodHandle;
        this.arguments = Collections.unmodifiableList(orderedParameterMetadata);
        this.customErrorMetadata = customErrorMetadata;
        this.returnType = methodHandle.type().returnType();
        this.cacheable = cacheable;
        this.strictArgsOrder = strictArgsOrder;
    }

    public MethodHandle getMethodHandle() {
        return methodHandle;
    }

    public List<ParameterMetadata> getArguments() {
        return arguments;
    }

    public boolean isCacheable() {
        return cacheable && !(returnType.equals(Void.class) || returnType.equals(Void.TYPE));
    }

    public boolean isStrictArgsOrder() {
        return strictArgsOrder;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    public CustomErrorMetadata getCustomErrorMetadata() {
        return customErrorMetadata;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final MethodMetadata that = (MethodMetadata) o;

        if (cacheable != that.cacheable) return false;
        if (strictArgsOrder != that.strictArgsOrder) return false;
        if (!Objects.equals(methodHandle, that.methodHandle)) return false;
        if (!Objects.equals(returnType, that.returnType)) return false;
        if (!Objects.equals(customErrorMetadata, that.customErrorMetadata)) return false;
        return Objects.equals(arguments, that.arguments);
    }

    @Override
    public int hashCode() {
        int result = methodHandle != null ? methodHandle.hashCode() : 0;
        result = 31 * result + (returnType != null ? returnType.hashCode() : 0);
        result = 31 * result + (cacheable ? 1 : 0);
        result = 31 * result + (strictArgsOrder ? 1 : 0);
        result = 31 * result + (customErrorMetadata != null ? customErrorMetadata.hashCode() : 0);
        result = 31 * result + (arguments != null ? arguments.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public String cacheName(final String postfix) {
        return methodHandle.type().toString() + '_' + postfix;
    }
}
