package com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi

import com.nextgenbroadcast.mobile.middleware.gateway.rpc.IRPCGateway
import com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.model.*

class ReceiverQueryApiImpl(
        private val gateway: IRPCGateway
) : IReceiverQueryApi {

    override fun queryContentAdvisoryRating(): RatingLevelRpcResponse {
        return RatingLevelRpcResponse()
    }

    override fun queryClosedCaptionsStatus(): CCRpcResponse {
        return CCRpcResponse()
    }

    override fun queryServiceID(): ServiceRpcResponse {
        return ServiceRpcResponse().apply {
            this.service = gateway.queryServiceId
        }
    }

    override fun queryLanguagePreferences(): LanguagesRpcResponse {
        return LanguagesRpcResponse().apply {
            gateway.language.let { language ->
                preferredAudioLang = language
                preferredCaptionSubtitleLang = language
                preferredUiLang = language
            }
        }
    }

    override fun queryCaptionDisplayPreferences(): CaptionDisplayRpcResponse {
        return CaptionDisplayRpcResponse()
    }

    override fun queryAudioAccessibilityPreferences(): AudioAccessibilityPrefRpcResponse {
        return AudioAccessibilityPrefRpcResponse()
    }

    override fun queryMPDUrl(): MPDUrlRpcResponse {
        return MPDUrlRpcResponse(gateway.mediaUrl)
    }

    override fun queryReceiverWebServerURI(): BaseURIRpcResponse {
        return BaseURIRpcResponse()
    }

    override fun queryAlertingSignaling(alertingTypes: List<String>): AlertingRpcResponse {
        return AlertingRpcResponse(gateway.getAlertChangingData(alertingTypes))
    }

    override fun queryServiceGuideURLs(service: String?): ServiceGuideUrlsRpcResponse {
        return ServiceGuideUrlsRpcResponse(gateway.getServiceGuideUrls(service))
    }
}