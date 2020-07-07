package org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi

import org.ngbp.jsonrpc4jtestharness.rpc.processor.RPCManager
import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model.*

class ReceiverQueryApiImpl(val rpcManager: RPCManager) : IReceiverQueryApi {
    override fun queryContentAdvisoryRating(): RatingLevel? {
        return null
    }

    override fun queryClosedCaptionsStatus(): CC? {
        return null
    }

    override fun queryServiceID(): Service? {
        return Service().apply {
            this.service = rpcManager.queryServiceId
        }
    }

    override fun queryLanguagePreferences(): Languages {
        return Languages().apply {
            this.preferredAudioLang = rpcManager.language
            this.preferredCaptionSubtitleLang = rpcManager.language
            this.preferredUiLang = rpcManager.language
        }
    }

    override fun queryCaptionDisplayPreferences(): CaptionDisplay? {
        return null
    }

    override fun queryAudioAccessibilityPreferences(): AudioAccessibilityPref? {
        return null
    }

    override fun queryReceiverWebServerURI(): BaseURI? {
        return null
    }

    override fun queryAlertingSignaling(): MutableList<Alerting?>? {
        return null
    }

    override fun queryServiceGuideURLs(): List<ServiceGuideUrls> {
        return rpcManager.listOfServiceGuideUrls
    }
}