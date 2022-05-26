package com.nextgenbroadcast.mobile.middleware.rpc

//import com.github.nmuzhichin.jsonrpc.internal.exceptions.CustomProcedureException

class RpcException : CustomProcedureException {
    constructor(): super()
    constructor(error: RpcErrorCode) : super(error.code, error.message)
}

open class CustomProcedureException : Throwable {
    constructor(): super()
    constructor(code: Int, message: String = "") : super(String.format("%d: %s", code, message))

}
