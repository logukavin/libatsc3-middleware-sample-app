package org.ngbp.jsonrpc4jtestharness.rpc.processor

import com.fasterxml.jackson.core.JsonProcessingException
import com.github.nmuzhichin.jsonrpc.api.Processor
import com.github.nmuzhichin.jsonrpc.api.RpcConsumer
import com.github.nmuzhichin.jsonrpc.context.ConsumerBuilder
import com.github.nmuzhichin.jsonrpc.model.request.Request
import com.github.nmuzhichin.jsonrpc.model.response.ResponseUtils
import com.github.nmuzhichin.jsonrpc.model.response.errors.Error
import org.ngbp.jsonrpc4jtestharness.gateway.rpc.IRPCGateway
import org.ngbp.jsonrpc4jtestharness.rpc.RpcError
import org.ngbp.jsonrpc4jtestharness.rpc.RpcErrorCode
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
import org.ngbp.jsonrpc4jtestharness.rpc.subscribeUnsubscribe.SubscribeUnsubscribeImpl
import org.ngbp.jsonrpc4jtestharness.rpc.xLink.IXLink
import org.ngbp.jsonrpc4jtestharness.rpc.xLink.XLinkImpl
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RPCProcessor @Inject constructor(
        private val gateway: IRPCGateway
) : IRPCProcessor {

    private val consumer: RpcConsumer

    init {
        consumer = ConsumerBuilder().build().also {
            filRequests(it.processor)
        }
    }

    private fun filRequests(processor: Processor) {
        processor.process(FilterCodesImpl(), IFilterCodes::class.java)
        processor.process(CacheRequestImpl(), ICacheRequest::class.java)
        processor.process(ContentRecoveryImpl(), IContentRecovery::class.java)
        processor.process(DRMImpl(), IDRM::class.java)
        processor.process(EventStreamImpl(), IEventStream::class.java)
        processor.process(KeysImpl(), IKeys::class.java)
        processor.process(MarkUnusedImpl(), IMarkUnused::class.java)
        processor.process(MediaTrackSelectionImpl(), IMediaTrackSelection::class.java)
        processor.process(QueryDeviceInfoImpl(), IQueryDeviceInfo::class.java)
        processor.process(ReceiverQueryApiImpl(gateway), IReceiverQueryApi::class.java)
        processor.process(ReceiverActionImpl(gateway), IReceiverAction::class.java)
        processor.process(RMPContentSynchronizationImpl(gateway), IRMPContentSynchronization::class.java)
        processor.process(SubscribeUnsubscribeImpl(gateway), ISubscribeUnsubscribe::class.java)
        processor.process(XLinkImpl(), IXLink::class.java)
    }

    override fun processRequest(request: String): String {
        var requestId: Long = -1L
        return try {
            val req = RPCObjectMapperUtils().jsonToObject(request, Request::class.java).also {
                requestId = it.id
            }

            val response = consumer.execution(req)

            RPCObjectMapperUtils().objectToJson(response)
        } catch (e: JsonProcessingException) {
            errorResponse(requestId, e)
        }
    }

    override fun processRequest(requests: List<String>): List<String> {
        return try {
            val requestList = requests.map { RPCObjectMapperUtils().jsonToObject(it, Request::class.java) }

            val responseList = consumer.execution(requestList)

            responseList.map { RPCObjectMapperUtils().objectToJson(it) }
        } catch (e: JsonProcessingException) {
            e.printStackTrace()
            ArrayList<String>(requests.size).apply {
                //TODO: we should return error's with request ID's
                fill("")
            }
        }
    }

    private fun errorResponse(requestId: Long, e: Exception): String {
        return try {
            RPCObjectMapperUtils().objectToJson(ResponseUtils.createResponse(requestId, InternalRpcError(RpcErrorCode.PARSING_ERROR_CODE.code, e.localizedMessage)))
        } catch (ex: JsonProcessingException) {
            // This catch will never been executed during code logic, but it need because objectMapper throw exception
            ""
        }
    }

    internal class InternalRpcError(code: Int, message: String?) : RpcError(code, message), Error {
        override fun getData(): Any? {
            return null
        }
    }
}