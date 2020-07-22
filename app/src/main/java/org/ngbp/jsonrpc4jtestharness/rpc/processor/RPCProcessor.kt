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
import com.github.nmuzhichin.jsonrpc.model.response.ResponseUtils
import com.github.nmuzhichin.jsonrpc.model.response.errors.Error
import com.github.nmuzhichin.jsonrpc.module.JsonRpcModule
import org.ngbp.jsonrpc4jtestharness.controller.IRPCController
import org.ngbp.jsonrpc4jtestharness.rpc.RpcException
import org.ngbp.jsonrpc4jtestharness.rpc.RpcErrorCode
import org.ngbp.jsonrpc4jtestharness.rpc.RpcError
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
import org.ngbp.jsonrpc4jtestharness.rpc.notification.IRPCNotification
import org.ngbp.jsonrpc4jtestharness.rpc.notification.RPCNotification
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
import java.lang.Exception
import javax.inject.Inject

class RPCProcessor @Inject constructor(
        private val rpcController: IRPCController
) : IRPCProcessor {

    private val consumer: RpcConsumer
    private val objectMapper: ObjectMapper

    init {
        consumer = ConsumerBuilder().build().also {
            filRequests(it.processor)
        }

        objectMapper = ObjectMapper().apply {
            setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            registerModule(JsonRpcModule())
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
        processor.process(ReceiverQueryApiImpl(rpcController), IReceiverQueryApi::class.java)
        processor.process(ReceiverActionImpl(rpcController), IReceiverAction::class.java)
        processor.process(RMPContentSynchronizationImpl(rpcController), IRMPContentSynchronization::class.java)
        processor.process(SubscribeUnsubscribeImp(), ISubscribeUnsubscribe::class.java)
        processor.process(XLinkImpl(), IXLink::class.java)
        processor.process(RPCNotification(rpcController), IRPCNotification::class.java)
    }

    override fun processRequest(request: String): String {
        var requestId: Long = -1L
        return try {
            val req = jsonToRequest(request).also {
                requestId = it.id
            }

            val response = consumer.execution(req).let { response ->
                var result = response
                if (response.isError) {
                    val error = response.error.get()
                    if (error.data is RpcException) {
                        RpcException.getRpcErrorCode(error.message)?.let { errorCode ->
                            result = ResponseUtils.createResponse(requestId, InternalRpcError(errorCode.code, errorCode.message))
                        }
                    }
                }

                result
            }

            responseToJson(response)
        } catch (e: JsonProcessingException) {
            errorResponse(requestId, e)
        }
    }

    override fun processRequest(requests: List<String>): List<String> {
        return try {
            val requestList = requests.map { jsonToRequest(it) }

            val responseList = consumer.execution(requestList)

            responseList.map { responseToJson(it) }
        } catch (e: JsonProcessingException) {
            e.printStackTrace()
            ArrayList<String>(requests.size).apply {
                //TODO: we should return error's with request ID's
                fill("")
            }
        }
    }

    private fun jsonToRequest(json: String) = objectMapper.readValue(json, Request::class.java)

    private fun responseToJson(response: Response) = objectMapper.writeValueAsString(response)

    private fun errorResponse(requestId: Long, e: Exception): String {
        return try {
            responseToJson(ResponseUtils.createResponse(requestId, InternalRpcError(RpcErrorCode.PARSING_ERROR_CODE.code, e.localizedMessage)))
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