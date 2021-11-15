package com.nextgenbroadcast.mobile.middleware.rpc.requestReceiverActions

import com.nextgenbroadcast.mobile.middleware.rpc.RpcErrorCode
import com.nextgenbroadcast.mobile.middleware.rpc.RpcException
import com.nextgenbroadcast.mobile.middleware.rpc.RpcResponse
import com.nextgenbroadcast.mobile.middleware.rpc.requestReceiverActions.model.AudioVolume
import com.nextgenbroadcast.mobile.middleware.server.IApplicationSession

class ReceiverActionImpl(
    private val session: IApplicationSession
) : IReceiverAction {

    override fun acquireService(svcToAcquire: String): RpcResponse {
        val result = session.requestServiceChange(svcToAcquire)
        if (!result) throw RpcException(RpcErrorCode.SERVICE_NOT_FOUND)
        //throw RpcException(RpcErrorCode.SERVICE_NOT_AUTHORIZED)
        return RpcResponse()
    }

    override fun videoScalingAndPositioning(scaleFactor: java.lang.Double, xPos: java.lang.Double, yPos: java.lang.Double): RpcResponse {
        session.requestRMPPosition(scaleFactor.toDouble(), xPos.toDouble(), yPos.toDouble())
        return RpcResponse()
    }

    override fun setRMPURL(operation: String, rmpUrl: String?, rmpSyncTime: Double?): RpcResponse {
        //TODO: currently we do not support delays
        if (rmpSyncTime != null) {
            throw RpcException(RpcErrorCode.SYNCHRONIZATION_CANNOT_BE_ACHIEVED)
        }

        when (operation) {
            "startRmp" -> {
                session.requestMediaPlay(rmpUrl, convertSecToMilliSec(rmpSyncTime))
            }
            "stopRmp" -> {
                session.requestMediaStop(delay = convertSecToMilliSec(rmpSyncTime))
            }
            "resumeService" -> {
                session.requestMediaPlay(delay = convertSecToMilliSec(rmpSyncTime))
            }
        }

        return RpcResponse()
    }

    private fun convertSecToMilliSec(rmpSyncTime: Double?): Long {
        return rmpSyncTime?.let {
            if (rmpSyncTime == -1.0) {
                Long.MAX_VALUE
            } else {
                (rmpSyncTime * 1000).toLong()
            }
        } ?: 0
    }

    override fun audioVolume(): AudioVolume {
        return AudioVolume()
    }

}

