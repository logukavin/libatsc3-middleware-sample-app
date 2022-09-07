package com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi

import com.nextgenbroadcast.mobile.middleware.atsc3.Atsc3LLSTable
import com.nextgenbroadcast.mobile.middleware.rpc.RpcException
import com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.model.*
import com.nextgenbroadcast.mobile.middleware.server.IApplicationSession

class ReceiverQueryApiImpl(
    private val session: IApplicationSession
) : IReceiverQueryApi {

    override fun queryContentAdvisoryRating(): RatingLevelRpcResponse {
        return RatingLevelRpcResponse()
    }

    override fun queryClosedCaptionsStatus(): CaptionsRpcResponse {
        return CaptionsRpcResponse()
    }

    override fun queryServiceID(): ServiceRpcResponse {
        return ServiceRpcResponse().apply {
            this.service = session.getParam(IApplicationSession.Params.ServiceId)
        }
    }

    override fun queryLanguagePreferences(): LanguagesRpcResponse {
        val language = session.getParam(IApplicationSession.Params.Language)
        return LanguagesRpcResponse().apply {
            preferredAudioLang = language
            preferredCaptionSubtitleLang = language
            preferredUiLang = language
        }
    }

    override fun queryCaptionDisplayPreferences(): CaptionDisplayRpcResponse {
        return CaptionDisplayRpcResponse()
    }

    override fun queryAudioAccessibilityPreferences(): AudioAccessibilityPrefRpcResponse {
        return AudioAccessibilityPrefRpcResponse()
    }

    override fun queryMPDUrl(): MPDUrlRpcResponse {
        return MPDUrlRpcResponse(session.getParam(IApplicationSession.Params.MediaUrl))
    }

    override fun queryReceiverWebServerURI(): BaseURIRpcResponse {
        return session.getParam(IApplicationSession.Params.AppBaseUrl)?.let {
            BaseURIRpcResponse(it)
        } ?: throw RpcException()
    }

    override fun queryAlertingSignaling(alertingTypes: List<String>): AlertingSignalingRpcResponse {
        val aeatList = if (alertingTypes.contains(AlertingSignalingRpcResponse.Alert.AEAT)) {
            session.getAEATChangingList()
        } else emptyList()
        return AlertingSignalingRpcResponse(
            listOf(AlertingSignalingRpcResponse.Alert(AlertingSignalingRpcResponse.Alert.AEAT,
                aeatList.joinToString(separator = "", prefix = "<AEAT>", postfix = "</AEAT>") { it }))
        )
    }

    override fun queryServiceGuideURLs(service: String?): ServiceGuideUrlsRpcResponse {
        return ServiceGuideUrlsRpcResponse(

            session.getServiceGuideUrls(service).map { sgUrl ->
                ServiceGuideUrlsRpcResponse.Url(
                    sgUrl.sgType.toString(),
                    sgUrl.sgPath,
                    sgUrl.service,
                    sgUrl.content
                )
            }
        )
    }

    override fun querySignaling(names: List<String>): SignalingRpcResponse {
        return SignalingRpcResponse(
            session.getSignalingInfo(names).map { data ->
                SignalingRpcResponse.SignalingInfo(
                    name = data.name,
                    group = (data as? Atsc3LLSTable)?.groupId?.toString(),
                    version = data.version.toString(),
                    table = data.xml
                )
            }
        )
    }

}