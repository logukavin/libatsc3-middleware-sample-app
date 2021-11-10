package com.nextgenbroadcast.mobile.middleware.rpc.processor

interface IRPCProcessor {
    fun processRequest(payload: String): String
    fun processRequest(requests: List<String>): List<String>
}