package org.ngbp.jsonrpc4jtestharness.rsp.receiverQueryApi.api;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcService;

import org.ngbp.jsonrpc4jtestharness.models.JsonRpcResponse;
import org.ngbp.jsonrpc4jtestharness.rsp.receiverQueryApi.model.Alerting;
import org.ngbp.jsonrpc4jtestharness.rsp.receiverQueryApi.model.AudioAccessibilityPref;
import org.ngbp.jsonrpc4jtestharness.rsp.receiverQueryApi.model.BaseURI;
import org.ngbp.jsonrpc4jtestharness.rsp.receiverQueryApi.model.CC;
import org.ngbp.jsonrpc4jtestharness.rsp.receiverQueryApi.model.CaptionDisplay;
import org.ngbp.jsonrpc4jtestharness.rsp.receiverQueryApi.model.Languages;
import org.ngbp.jsonrpc4jtestharness.rsp.receiverQueryApi.model.RatingLevel;
import org.ngbp.jsonrpc4jtestharness.rsp.receiverQueryApi.model.Service;
import org.ngbp.jsonrpc4jtestharness.rsp.receiverQueryApi.model.ServiceGuideUrls;

import java.util.List;

@JsonRpcService("")
public interface ReceiverQueryApi {

    @JsonRpcMethod("org.atsc.query.ratingLevel")
    RatingLevel queryContentAdvisoryRating();

    @JsonRpcMethod("org.atsc.query.cc")
    CC queryClosedCaptionsStatus();

    @JsonRpcMethod("org.atsc.query.service")
    Service queryServiceID();

    @JsonRpcMethod("org.atsc.query.languages")
    Languages queryLanguagePreferences();

    @JsonRpcMethod("org.atsc.query.captionDisplay")
    CaptionDisplay queryCaptionDisplayPreferences();

    @JsonRpcMethod("org.atsc.query.audioAccessibilityPref")
    AudioAccessibilityPref queryAudioAccessibilityPreferences();

    @JsonRpcMethod("org.atsc.query.baseURI")
    BaseURI queryReceiverWebServerURI();

    @JsonRpcMethod("org.atsc.query.alerting")
    List<Alerting> queryAlertingSignaling();

    @JsonRpcMethod("org.atsc.query.serviceGuideUrls")
    List<ServiceGuideUrls> queryServiceGuideURLs ();
}
