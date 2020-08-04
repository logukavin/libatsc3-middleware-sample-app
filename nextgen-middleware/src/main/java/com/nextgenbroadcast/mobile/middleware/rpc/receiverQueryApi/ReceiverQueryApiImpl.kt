package com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi

import com.nextgenbroadcast.mobile.middleware.gateway.rpc.IRPCGateway
import com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.model.*

class ReceiverQueryApiImpl(
        private val gateway: IRPCGateway
) : IReceiverQueryApi {

    override fun queryContentAdvisoryRating(): RatingLevel {
        return RatingLevel()
    }

    override fun queryClosedCaptionsStatus(): CC {
        return CC()
    }

    override fun queryServiceID(): Service {
        return Service().apply {
            this.service = gateway.queryServiceId
        }
    }

    override fun queryLanguagePreferences(): Languages {
        return Languages().apply {
            gateway.language.let { language ->
                preferredAudioLang = language
                preferredCaptionSubtitleLang = language
                preferredUiLang = language
            }
        }
    }

    override fun queryCaptionDisplayPreferences(): CaptionDisplay {
        return CaptionDisplay()
    }

    override fun queryAudioAccessibilityPreferences(): AudioAccessibilityPref {
        return AudioAccessibilityPref()
    }

    override fun queryMPDUrl(): MPDUrl {
        return MPDUrl(gateway.mediaUrl)
    }

    override fun queryReceiverWebServerURI(): BaseURI {
        return BaseURI()
    }

    override fun queryAlertingSignaling(): Alerting {
        return Alerting()
    }

    override fun queryServiceGuideURLs(): ServiceGuideUrls {
        return ServiceGuideUrls(gateway.serviceGuideUrls)
    }
}