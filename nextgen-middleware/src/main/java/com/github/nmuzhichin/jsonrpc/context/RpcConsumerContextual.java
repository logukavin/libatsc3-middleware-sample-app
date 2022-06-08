package com.github.nmuzhichin.jsonrpc.context;

import com.github.nmuzhichin.jsonrpc.api.Context;
import com.github.nmuzhichin.jsonrpc.api.Processor;
import com.github.nmuzhichin.jsonrpc.api.RpcConsumer;
import com.github.nmuzhichin.jsonrpc.internal.logger.Logger;
import com.github.nmuzhichin.jsonrpc.model.request.Notification;
import com.github.nmuzhichin.jsonrpc.model.request.Request;
import com.github.nmuzhichin.jsonrpc.model.response.Response;
import com.github.nmuzhichin.jsonrpc.model.response.ResponseUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.stream.Collectors;

class RpcConsumerContextual implements RpcConsumer {
    private static final Logger log = Logger.of(RpcConsumerContextual.class);

    private final ExecutorService threadPool;
    private final Context context;

    RpcConsumerContextual(final Context context, final ExecutorService threadPool) {
        this.threadPool = threadPool;
        this.context = context;
    }

    @Override
    public void notify(final Notification notification) {
        threadPool.submit(() -> context.invoke(notification.getMethod(), notification.getParams()));
    }

    @Override
    public CompletableFuture<Response> asyncExecution(final Request request) {
        return CompletableFuture.supplyAsync(() -> execution(request), threadPool);
    }

    @Override
    public Response execution(final Request request) {
        return ResponseUtils.createResponse(request.getId(), context.invoke(request.getMethod(), request.getParams()));
    }

    @Override
    public List<Response> execution(final List<Request> requestBatch) {
        final List<Callable<Response>> callables = requestBatch
                .stream()
                .map(req -> (Callable<Response>) () -> execution(req))
                .collect(Collectors.toList());

        try {
            final List<Future<Response>> futures = threadPool.invokeAll(callables);
            final int awaitTime = futures.size() * 200_000;

            threadPool.awaitTermination(awaitTime, TimeUnit.NANOSECONDS);

            return futures.stream().map(it -> {
                try {
                    return it.get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error(e.getMessage(), e);

                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());
        } catch (Exception e) {
            log.error(e.getMessage(), e);

            return Collections.emptyList();
        }

    }

    @Override
    public Response[] execution(final Request[] requestBatch) {
        return execution(Arrays.asList(requestBatch)).toArray(new Response[requestBatch.length]);
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public Processor getProcessor() {
        return ((InvocationContextProcessor) context);
    }
}
