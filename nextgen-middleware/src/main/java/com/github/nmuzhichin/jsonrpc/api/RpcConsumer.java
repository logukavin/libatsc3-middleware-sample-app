package com.github.nmuzhichin.jsonrpc.api;

import com.github.nmuzhichin.jsonrpc.model.request.Notification;
import com.github.nmuzhichin.jsonrpc.model.request.Request;
import com.github.nmuzhichin.jsonrpc.model.response.Response;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface RpcConsumer {
    /**
     * A request without an identifier will be executed as a notification (the client does not receive a response)
     * Method calling as asynchronous task.
     *
     * Can use {@link com.github.nmuzhichin.jsonrpc.model.request.RequestUtils} for create request
     * with addition parameters.
     */
    void notify(Notification notification);

    /**
     * Remote call with specified request.
     * Can use {@link com.github.nmuzhichin.jsonrpc.model.request.RequestUtils} for create request
     * with addition parameters.
     */
    Response execution(Request request);

    /**
     * Async remote call.
     * Can use {@link com.github.nmuzhichin.jsonrpc.model.request.RequestUtils} for create request
     * with addition parameters.
     */
    CompletableFuture<Response> asyncExecution(Request request);

    /**
     * Batch request (list)
     */
    List<Response> execution(List<Request> requestBatch);

    /**
     * Batch request (array)
     */
    Response[] execution(Request[] requestBatch);

    /**
     * Get context
     */
    Context getContext();

    /**
     * Get processor
     */
    Processor getProcessor();
}
