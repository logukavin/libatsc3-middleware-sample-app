package org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType
import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model.*

@JsonRpcType
interface IReceiverQueryApi {
    @JsonRpcMethod("org.atsc.query.ratingLevel")
    fun queryContentAdvisoryRating(): RatingLevel

    @JsonRpcMethod("org.atsc.query.cc")
    fun queryClosedCaptionsStatus(): CC

    @JsonRpcMethod("org.atsc.query.service")
    fun queryServiceID(): Service

    @JsonRpcMethod("org.atsc.query.languages")
    fun queryLanguagePreferences(): Languages

    @JsonRpcMethod("org.atsc.query.captionDisplay")
    fun queryCaptionDisplayPreferences(): CaptionDisplay

    @JsonRpcMethod("org.atsc.query.audioAccessibilityPref")
    fun queryAudioAccessibilityPreferences(): AudioAccessibilityPref

    @JsonRpcMethod("org.atsc.query.MPDUrl")
    fun queryMPDURL(): MPDUrl

    @JsonRpcMethod("org.atsc.query.baseURI")
    fun queryReceiverWebServerURI(): BaseURI

    @JsonRpcMethod("org.atsc.query.alerting")
    fun queryAlertingSignaling(): Alerting

    @JsonRpcMethod("org.atsc.query.serviceGuideUrls")
    fun queryServiceGuideURLs(): ServiceGuideUrls
}