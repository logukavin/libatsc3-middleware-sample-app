package org.ngbp.jsonrpc4jtestharness.rpc.processor

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.nmuzhichin.jsonrpc.api.Processor
import com.github.nmuzhichin.jsonrpc.api.RpcConsumer
import com.github.nmuzhichin.jsonrpc.context.ConsumerBuilder
import com.github.nmuzhichin.jsonrpc.model.request.Request
import com.github.nmuzhichin.jsonrpc.model.response.Response
import com.github.nmuzhichin.jsonrpc.model.response.errors.Error
import com.github.nmuzhichin.jsonrpc.module.JsonRpcModule
import org.ngbp.jsonrpc4jtestharness.rpc.ERROR_CODES
import org.ngbp.jsonrpc4jtestharness.rpc.asynchronousNotificationsofChanges.AsynchronousNotificationsOfChangesImpl
import org.ngbp.jsonrpc4jtestharness.rpc.asynchronousNotificationsofChanges.IAsynchronousNotificationsOfChanges
import org.ngbp.jsonrpc4jtestharness.rpc.cacheRequest.CacheRequestImpl
import org.ngbp.jsonrpc4jtestharness.rpc.cacheRequest.ICacheRequest
import org.ngbp.jsonrpc4jtestharness.rpc.contentRecovery.ContentRecoveryImpl
import org.ngbp.jsonrpc4jtestharness.rpc.contentRecovery.IContentRecovery
import org.ngbp.jsonrpc4jtestharness.rpc.drm.DRMImpl
import org.ngbp.jsonrpc4jtestharness.rpc.drm.IDRM
import org.ngbp.jsonrpc4jtestharness.rpc.eventStream.EventStreamImpl
import org.ngbp.jsonrpc4jtestharness.rpc.eventStream.IEventStream
import org.ngbp.jsonrpc4jtestharness.rpc.filterCodes.FilterCodesImpl
import org.ngbp.jsonrpc4jtestharness.rpc.filterCodes.IFilterCodes
import org.ngbp.jsonrpc4jtestharness.rpc.keys.IKeys
import org.ngbp.jsonrpc4jtestharness.rpc.keys.KeysImpl
import org.ngbp.jsonrpc4jtestharness.rpc.markUnused.IMarkUnused
import org.ngbp.jsonrpc4jtestharness.rpc.markUnused.MarkUnusedImpl
import org.ngbp.jsonrpc4jtestharness.rpc.mediaTrackSelection.IMediaTrackSelection
import org.ngbp.jsonrpc4jtestharness.rpc.mediaTrackSelection.MediaTrackSelectionImpl
import org.ngbp.jsonrpc4jtestharness.rpc.queryDeviceInf.IQueryDeviceInfo
import org.ngbp.jsonrpc4jtestharness.rpc.queryDeviceInf.QueryDeviceInfoImpl
import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.IReceiverQueryApi
import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.ReceiverQueryApiImpl
import org.ngbp.jsonrpc4jtestharness.rpc.requestReceiverActions.IReceiverAction
import org.ngbp.jsonrpc4jtestharness.rpc.requestReceiverActions.ReceiverActionImpl
import org.ngbp.jsonrpc4jtestharness.rpc.rmpContentSynchronization.IRMPContentSynchronization
import org.ngbp.jsonrpc4jtestharness.rpc.rmpContentSynchronization.RMPContentSynchronizationImpl
import org.ngbp.jsonrpc4jtestharness.rpc.subscribeUnsubscribe.ISubscribeUnsubscribe
import org.ngbp.jsonrpc4jtestharness.rpc.subscribeUnsubscribe.SubscribeUnsubscribeImp
import org.ngbp.jsonrpc4jtestharness.rpc.xLink.IXLink
import org.ngbp.jsonrpc4jtestharness.rpc.xLink.XLinkImpl
import java.util.*

class RPCProcessor(rpcManager: RPCManager) : IRPCProcessor {
    private val consumer: RpcConsumer
    private val processor: Processor
    private val objectMapper: ObjectMapper = ObjectMapper()
    private val rpcManager: RPCManager

    init {
        consumer = ConsumerBuilder().build().also {
            processor = it.processor
        }

        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        objectMapper.registerModule(JsonRpcModule())

        this.rpcManager = rpcManager

        filRequests()
    }

    private fun filRequests() {
        processor.process(FilterCodesImpl(), IFilterCodes::class.java)
        processor.process(AsynchronousNotificationsOfChangesImpl(), IAsynchronousNotificationsOfChanges::class.java)
        processor.process(CacheRequestImpl(), ICacheRequest::class.java)
        processor.process(ContentRecoveryImpl(), IContentRecovery::class.java)
        processor.process(DRMImpl(), IDRM::class.java)
        processor.process(EventStreamImpl(), IEventStream::class.java)
        processor.process(KeysImpl(), IKeys::class.java)
        processor.process(MarkUnusedImpl(), IMarkUnused::class.java)
        processor.process(MediaTrackSelectionImpl(), IMediaTrackSelection::class.java)
        processor.process(QueryDeviceInfoImpl(rpcManager), IQueryDeviceInfo::class.java)
        processor.process(ReceiverQueryApiImpl(), IReceiverQueryApi::class.java)
        processor.process(ReceiverActionImpl(rpcManager), IReceiverAction::class.java)
        processor.process(RMPContentSynchronizationImpl(), IRMPContentSynchronization::class.java)
        processor.process(SubscribeUnsubscribeImp(), ISubscribeUnsubscribe::class.java)
        processor.process(XLinkImpl(), IXLink::class.java)
    }

    override fun processRequest(request: String): String {
        var response: Response? = null
        var requestId: Long? = -1L
        return try {
            requestId = objectMapper.readValue(request, Request::class.java).id
            response = consumer.execution(objectMapper.readValue(request, Request::class.java))
            objectMapper.writeValueAsString(response)
        } catch (e: JsonProcessingException) {
            try {
                objectMapper.writeValueAsString(Response.createResponse(requestId, InternalRpcError(ERROR_CODES.PARSING_ERROR_CODE.value, e.localizedMessage)))
            } catch (ex: JsonProcessingException) {
                // This catch will never been executed during code logic, but it need because objectMapper throw exception
                ""
            }
        }
    }

    override fun processRequest(requests: MutableList<String?>): MutableList<String?> {
        val wrappedList: MutableList<String?> = ArrayList(requests.size)
        try {
            val requestList: MutableList<Request?> = ArrayList()
            for (i in requests.indices) {
                requestList.add(objectMapper.readValue(requests.get(i), Request::class.java))
            }
            val responseList = consumer.execution(requestList)
            for (r in responseList) {
                wrappedList.add(objectMapper.writeValueAsString(r.body))
            }
        } catch (e: JsonProcessingException) {
            e.printStackTrace()
        }
        return wrappedList
    }

    internal class InternalRpcError(code: Int, message: String?) : RpcError(code, message), Error {
        override fun getData(): Any? {
            return null
        }
    }
}