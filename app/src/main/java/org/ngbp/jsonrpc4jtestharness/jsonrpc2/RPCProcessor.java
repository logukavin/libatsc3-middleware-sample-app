package org.ngbp.jsonrpc4jtestharness.jsonrpc2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.nmuzhichin.jsonrpc.api.Processor;
import com.github.nmuzhichin.jsonrpc.api.RpcConsumer;
import com.github.nmuzhichin.jsonrpc.context.ConsumerBuilder;
import com.github.nmuzhichin.jsonrpc.model.request.Request;
import com.github.nmuzhichin.jsonrpc.model.response.Response;
import com.github.nmuzhichin.jsonrpc.module.JsonRpcModule;

import org.ngbp.jsonrpc4jtestharness.rpc.asynchronousNotificationsofChanges.AsynchronousNotificationsOfChangesImpl;
import org.ngbp.jsonrpc4jtestharness.rpc.asynchronousNotificationsofChanges.IAsynchronousNotificationsOfChanges;
import org.ngbp.jsonrpc4jtestharness.rpc.cacheRequest.CacheRequestImpl;
import org.ngbp.jsonrpc4jtestharness.rpc.cacheRequest.ICacheRequest;
import org.ngbp.jsonrpc4jtestharness.rpc.contentRecovery.ContentRecoveryImpl;
import org.ngbp.jsonrpc4jtestharness.rpc.contentRecovery.IContentRecovery;
import org.ngbp.jsonrpc4jtestharness.rpc.drm.DRMImpl;
import org.ngbp.jsonrpc4jtestharness.rpc.drm.IDRM;
import org.ngbp.jsonrpc4jtestharness.rpc.eventStream.EventStreamImpl;
import org.ngbp.jsonrpc4jtestharness.rpc.eventStream.IEventStream;
import org.ngbp.jsonrpc4jtestharness.rpc.filterCodes.FilterCodesImpl;
import org.ngbp.jsonrpc4jtestharness.rpc.filterCodes.IFilterCodes;
import org.ngbp.jsonrpc4jtestharness.rpc.keys.IKeys;
import org.ngbp.jsonrpc4jtestharness.rpc.keys.KeysImpl;
import org.ngbp.jsonrpc4jtestharness.rpc.markUnused.IMarkUnused;
import org.ngbp.jsonrpc4jtestharness.rpc.markUnused.MarkUnusedImpl;
import org.ngbp.jsonrpc4jtestharness.rpc.mediaTrackSelection.IMediaTrackSelection;
import org.ngbp.jsonrpc4jtestharness.rpc.mediaTrackSelection.MediaTrackSelectionImpl;
import org.ngbp.jsonrpc4jtestharness.rpc.queryDeviceInf.IQueryDeviceInfo;
import org.ngbp.jsonrpc4jtestharness.rpc.queryDeviceInf.QueryDeviceInfoImpl;
import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.IReceiverQueryApi;
import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.ReceiverQueryApiImpl;
import org.ngbp.jsonrpc4jtestharness.rpc.requestReceiverActions.IRequestReceiverActions;
import org.ngbp.jsonrpc4jtestharness.rpc.requestReceiverActions.RequestReceiverActionsImpl;
import org.ngbp.jsonrpc4jtestharness.rpc.rmpContentSynchronization.IRMPContentSynchronization;
import org.ngbp.jsonrpc4jtestharness.rpc.rmpContentSynchronization.RMPContentSynchronizationImpl;
import org.ngbp.jsonrpc4jtestharness.rpc.subscribeUnsubscribe.ISubscribeUnsubscribe;
import org.ngbp.jsonrpc4jtestharness.rpc.subscribeUnsubscribe.SubscribeUnsubscribeImp;
import org.ngbp.jsonrpc4jtestharness.rpc.xLink.IXLink;
import org.ngbp.jsonrpc4jtestharness.rpc.xLink.XLinkImpl;

import java.util.ArrayList;
import java.util.List;

public class RPCProcessor implements IRPCProcessor {
    private final RpcConsumer consumer;
    private final Processor processor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RPCProcessor() {
        consumer = new ConsumerBuilder()
                .build();
        processor = consumer.getProcessor();
        objectMapper.registerModule(new JsonRpcModule());
        filRequests();
    }

    private void filRequests() {
        processor.process(new FilterCodesImpl(), IFilterCodes.class);
        processor.process(new AsynchronousNotificationsOfChangesImpl(), IAsynchronousNotificationsOfChanges.class);
        processor.process(new CacheRequestImpl(), ICacheRequest.class);
        processor.process(new ContentRecoveryImpl(), IContentRecovery.class);
        processor.process(new DRMImpl(), IDRM.class);
        processor.process(new EventStreamImpl(), IEventStream.class);
        processor.process(new KeysImpl(), IKeys.class);
        processor.process(new MarkUnusedImpl(), IMarkUnused.class);
        processor.process(new MediaTrackSelectionImpl(), IMediaTrackSelection.class);
        processor.process(new QueryDeviceInfoImpl(), IQueryDeviceInfo.class);
        processor.process(new ReceiverQueryApiImpl(), IReceiverQueryApi.class);
        processor.process(new RequestReceiverActionsImpl(), IRequestReceiverActions.class);
        processor.process(new RMPContentSynchronizationImpl(), IRMPContentSynchronization.class);
        processor.process(new SubscribeUnsubscribeImp(), ISubscribeUnsubscribe.class);
        processor.process(new XLinkImpl(), IXLink.class);
    }

    @Override
    public <T> T processRequest(String request) {
        Response response = null;
        try {
            response = consumer.execution(objectMapper.readValue(request, Request.class));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        if (response != null && response.isSuccess()) {
            return (T) response.getBody();
        } else {
            return null;
        }
    }

    @Override
    public List<Object> processRequest(List<String> requests) {
        List<Object> wrappedList = new ArrayList<>(requests.size());
        try {
            List<Request> responseList = new ArrayList<>();
            for (int i = 0; i < requests.size(); i++) {
                responseList.add(objectMapper.readValue(requests.get(i), Request.class));
            }
            final List<Response> response = consumer.execution(responseList);
            for (int i = 0; i < response.size(); i++) {
                wrappedList.set(i, response.get(i).getBody());
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return wrappedList;
    }

}
