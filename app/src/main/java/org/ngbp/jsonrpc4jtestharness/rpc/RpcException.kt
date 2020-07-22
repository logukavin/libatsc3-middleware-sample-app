package org.ngbp.jsonrpc4jtestharness.rpc

import java.lang.NumberFormatException

class RpcException : RuntimeException {
    constructor(): super()
    constructor(error: RpcErrorCode) : super(error.code.toString())

    companion object {
        fun getRpcErrorCode(msg: String): RpcErrorCode? {
            val code = try {
                msg.toInt()
            } catch (e: NumberFormatException) {
                return null
            }
            return RpcErrorCode.values().firstOrNull { it.code == code }
        }
    }
}
