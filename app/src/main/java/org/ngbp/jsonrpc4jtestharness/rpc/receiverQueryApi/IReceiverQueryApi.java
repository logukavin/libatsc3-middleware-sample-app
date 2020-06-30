package org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi;

import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcMethod;
import com.github.nmuzhichin.jsonrpc.annotation.JsonRpcType;

import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model.Alerting;
import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model.AudioAccessibilityPref;
import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model.BaseURI;
import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model.CC;
import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model.CaptionDisplay;
import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model.Languages;
import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model.RatingLevel;
import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model.Service;
import org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model.ServiceGuideUrls;

import java.util.List;

@JsonRpcType
public interface IReceiverQueryApi {

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
    List<ServiceGuideUrls> queryServiceGuideURLs();
}
