package com.nextgenbroadcast.mobile.middleware.rpc.processor

import android.util.Log
import com.fasterxml.jackson.core.JsonProcessingException
import com.github.nmuzhichin.jsonrpc.api.RpcConsumer
import com.github.nmuzhichin.jsonrpc.model.request.Request
import com.github.nmuzhichin.jsonrpc.model.response.ResponseUtils
import com.github.nmuzhichin.jsonrpc.model.response.errors.Error
import com.nextgenbroadcast.mobile.middleware.rpc.RpcError
import com.nextgenbroadcast.mobile.middleware.rpc.RpcErrorCode

internal abstract class AbstractRPCProcessor(
    private val consumer: RpcConsumer
) : IRPCProcessor {

    private val rpcObjectMapper = RPCObjectMapper()

    override fun processRequest(payload: String): String {
        var requestId: Long = -1L
        return try {
            val request = rpcObjectMapper.jsonToObject(payload, Request::class.java).also { request ->
                requestId = request.id
            }

            val response = consumer.execution(request)

            if(response.isError) {
                Log.w(TAG, "processRequest: response.isError: requestId: $requestId, request: $payload, $response")
            }

            rpcObjectMapper.objectToJson(response)
        } catch (e: JsonProcessingException) {
            Log.e(TAG, "processRequest: exception: requestId: $requestId, request: $payload", e)
            errorResponse(requestId, e)
        }
    }

    override fun processRequest(requests: List<String>): List<String> {
        return try {
            val requestList = requests.map { rpcObjectMapper.jsonToObject(it, Request::class.java) }

            val responseList = consumer.execution(requestList)

            responseList.map { rpcObjectMapper.objectToJson(it) }
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
            rpcObjectMapper.objectToJson(ResponseUtils.createResponse(requestId, InternalRpcError(RpcErrorCode.PARSING_ERROR_CODE.code, e.localizedMessage)))
        } catch (ex: JsonProcessingException) {
            // This catch should never being executed, but it's need because objectMapper throw exception
            ""
        }
    }

    internal class InternalRpcError(code: Int, message: String?) : RpcError(code, message), Error {
        override fun getData(): Any? {
            return null
        }
    }

    companion object {
        val TAG: String = AbstractRPCProcessor::class.java.simpleName
    }
}