package com.github.nmuzhichin.jsonrpc.context;

import com.github.nmuzhichin.jsonrpc.api.Context;
import com.github.nmuzhichin.jsonrpc.api.MutateListener;
import com.github.nmuzhichin.jsonrpc.api.Processor;
import com.github.nmuzhichin.jsonrpc.cache.CacheProvider;
import com.github.nmuzhichin.jsonrpc.internal.function.ExceptionSuppressor;
import com.github.nmuzhichin.jsonrpc.internal.logger.Logger;
import com.github.nmuzhichin.jsonrpc.model.response.errors.Error;
import com.github.nmuzhichin.jsonrpc.model.response.errors.MeaningError;
import com.github.nmuzhichin.jsonrpc.normalizer.ValueNormalization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.nmuzhichin.jsonrpc.model.response.errors.Error.Predefine.METHOD_NOT_FOUND;

public final class InvocationContextProcessor implements Context, Processor {
    private static final Logger log = Logger.of(InvocationContextProcessor.class);
    /**
     * Store methods metadata
     */
    private final Map<String, MethodMetadata> methodMetadataStorage;
    /**
     * Store cache provider
     */
    private final CacheProvider cacheProvider;
    /**
     * Invoke with arguments interceptor
     */
    private final InvokeArgumentInterceptor invokeArgumentInterceptor;
    /**
     * Error interceptor
     */
    private final InvokeErrorInterceptor invokeErrorInterceptor;
    /**
     * Result interceptor
     */
    private final InvokeResultInterceptor invokeResultInterceptor;

    InvocationContextProcessor(final Map<String, MethodMetadata> methodMetadataStorage,
                               final CacheProvider cacheProvider,
                               final ValueNormalization normalization,
                               final MutateListener mutateListener) {

        this.methodMetadataStorage = methodMetadataStorage;
        this.cacheProvider = cacheProvider;
        this.invokeArgumentInterceptor = new InvokeArgumentInterceptor(normalization);
        this.invokeErrorInterceptor = new InvokeErrorInterceptor();
        this.invokeResultInterceptor = new InvokeResultInterceptor(mutateListener);
    }

    @Override
    public void process(final Object object, final Class<?> objectType) {
        methodMetadataStorage.putAll(AnnotationLookup.lookupMethodAnnotation(object, objectType));
    }

    @Override
    public Object invoke(final String methodName, final Map<String, Object> params) {
        final MethodMetadata methodMetadata = methodMetadataStorage.get(methodName);

        final Object invokeResult;
        if (methodMetadata == null) {
            log.error(METHOD_NOT_FOUND.getMeaning());
            invokeResult = new MeaningError(METHOD_NOT_FOUND);
        } else {
            final Map<String, Object> nonNullParams =
                    params == null ? new HashMap<>(methodMetadata.getArguments().size()) : params;

            if (methodMetadata.isCacheable()) {
                final Object value = cacheProvider.get(methodMetadata.cacheName(nonNullParams.toString()));

                if (value != null) {
                    invokeResult = value;
                } else {
                    invokeResult = methodHandleInvoke(nonNullParams, methodMetadata);
                }
            } else {
                invokeResult = methodHandleInvoke(nonNullParams, methodMetadata);
            }
        }

        return invokeResultInterceptor.intercept(invokeResult, null);
    }

    private Object methodHandleInvoke(final Map<String, Object> params, final MethodMetadata mInfo) {
        final ExceptionSuppressor<Object> suppressor;
        if (mInfo.getArguments().isEmpty()) {
            suppressor = () -> mInfo.getMethodHandle().invoke();
        } else {
            if (mInfo.isStrictArgsOrder()) {
                suppressor = () -> mInfo.getMethodHandle().invokeWithArguments(new ArrayList<>(params.values()));
            } else {
                final List<Object> args = invokeArgumentInterceptor.intercept(mInfo.getArguments(), params);
                suppressor = () -> mInfo.getMethodHandle().invokeWithArguments(args);
            }
        }

        final Object v = invokeErrorInterceptor.intercept(suppressor, mInfo.getCustomErrorMetadata());

        if (mInfo.isCacheable() && !(v instanceof Error)) {
            cacheProvider.put(mInfo.cacheName(params.toString()), v);
        }

        return v;
    }
}