package com.github.nmuzhichin.jsonrpc.context;

import com.github.nmuzhichin.jsonrpc.api.Interceptor;
import com.github.nmuzhichin.jsonrpc.api.MutateListener;

import javax.annotation.Nullable;

final class InvokeResultInterceptor implements Interceptor<Object, Void, Object> {

    private final MutateListener mutateListener;

    InvokeResultInterceptor(final MutateListener mutateListener) {
        this.mutateListener = mutateListener;
    }

    @Override
    public Object intercept(final Object invokeResult, @Nullable final Void ignored) {
        mutateListener.beforeMutate(invokeResult);
        final Object mutation = mutateListener.mutateResultState(invokeResult);
        mutateListener.afterMutate(mutation);

        return mutation;
    }
}
