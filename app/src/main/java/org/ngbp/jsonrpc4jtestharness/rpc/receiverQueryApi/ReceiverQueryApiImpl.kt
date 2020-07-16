package org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi

import org.ngbp.jsonrpc4jtestharness.controller.IRPCController
import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model.*

class ReceiverQueryApiImpl(
        private val rpcController: IRPCController
) : IReceiverQueryApi {

    override fun queryContentAdvisoryRating(): RatingLevel {
        return RatingLevel()
    }

    override fun queryClosedCaptionsStatus(): CC {
        return CC()
    }

    override fun queryServiceID(): Service {
        return Service().apply {
            this.service = rpcController.queryServiceId
        }
    }

    override fun queryLanguagePreferences(): Languages {
        return Languages().apply {
            rpcController.language.let { language ->
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
        return MPDUrl(rpcController.mediaUrl)
    }

    override fun queryReceiverWebServerURI(): BaseURI {
        return BaseURI()
    }

    override fun queryAlertingSignaling(): Alerting {
        return Alerting()
    }

    override fun queryServiceGuideURLs(): ServiceGuideUrls {
        return ServiceGuideUrls()
    }
}