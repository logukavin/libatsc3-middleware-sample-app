package org.ngbp.jsonrpc4jtestharness.rpc

import com.github.nmuzhichin.jsonrpc.internal.exceptions.CustomProcedureException

class RpcException : CustomProcedureException {
    constructor(): super()
    constructor(error: RpcErrorCode) : super(error.code, error.message)
}
