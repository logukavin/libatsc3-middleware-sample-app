package com.github.nmuzhichin.jsonrpc.context;

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcError;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.Objects;

public final class CustomErrorMetadata implements Metadata {
    private final Class<? extends Throwable> customErrorClass;
    private final JsonRpcError.Mode errorMode;
    private final Throwable[] acquiredException;

    CustomErrorMetadata(@Nullable final Class<? extends Throwable> customErrorClass,
                        @Nullable final JsonRpcError.Mode errorMode) {

        this.customErrorClass = customErrorClass;
        this.errorMode = errorMode;
        this.acquiredException = new Throwable[1];
    }

    CustomErrorMetadata(@Nullable final JsonRpcError jsonRpcError) {
        this(jsonRpcError == null ? null : jsonRpcError.value(),
             jsonRpcError == null ? null : jsonRpcError.mode());
    }

    @CheckForNull
    public Class<? extends Throwable> getCustomErrorClass() {
        return customErrorClass;
    }

    @CheckForNull
    public JsonRpcError.Mode getErrorMode() {
        return errorMode;
    }

    public boolean isCustomError() {
        return customErrorClass != null && errorMode != null;
    }

    public Throwable getAcquiredException() {
        return acquiredException[0];
    }

    public void setAcquiredException(final Throwable acquiredException) {
        this.acquiredException[0] = acquiredException;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final CustomErrorMetadata that = (CustomErrorMetadata) o;

        return errorMode == that.errorMode && Objects.equals(customErrorClass, that.customErrorClass);
    }

    @Override
    public int hashCode() {
        int result = customErrorClass != null ? customErrorClass.hashCode() : 0;
        result = 31 * result + (errorMode != null ? errorMode.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public String cacheName(final String postfix) {
        return (errorMode != null ? errorMode.toString() : "null")
               + '_'
               + (customErrorClass != null ? customErrorClass.getName() : "null")
               + '_'
               + postfix;
    }
}
