package com.github.nmuzhichin.jsonrpc.model.request;

import com.github.nmuzhichin.jsonrpc.internal.asserts.Assert;
import com.github.nmuzhichin.jsonrpc.internal.bijections.Bijections;

import java.util.HashMap;

/**
 * Helper is creating a new request with additional parameters.
 */
public abstract class RequestUtils {
    /**
     * Add predefine parameter to request.
     * Create new request.
     */
    public static Request copyWithParameter(final Request request, final Bijections<String, Object> bijections) {
        Assert.requireNotNull(request, "Request must be present.");
        Assert.requireNotNull(bijections, "Bijections must be present.");

        final HashMap<String, Object> newParameters = new HashMap<>(request.getParams());
        newParameters.putAll(bijections.toMap());

        return ((AbstractRequest) request).copy(newParameters);
    }

    /**
     * Add predefine parameter to request.
     * Create new request.
     */
    public static Request copyWithParameter(final Request request, final String parameterName, final Object object) {
        return copyWithParameter(request, Bijections.of(parameterName, object));
    }

    /**
     * Add predefine parameter to request.
     * Create new request.
     */
    public static Request copyWithParameter(final Request request, final Class<?> clazz, final Object object) {
        return copyWithParameter(request, Bijections.of(clazz.getName(), object));
    }

    /**
     * Add predefine parameter to request.
     * Create new request.
     */
    public static Request copyWithParameter(final Request request, final Object... object) {
        return copyWithParameter(request, Bijections.of(object));
    }
}
