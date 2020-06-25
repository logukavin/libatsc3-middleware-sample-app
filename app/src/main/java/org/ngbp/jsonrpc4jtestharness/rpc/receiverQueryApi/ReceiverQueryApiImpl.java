package org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi;

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

public class ReceiverQueryApiImpl implements IReceiverQueryApi {

    @Override
    public RatingLevel queryContentAdvisoryRating() {
        return null;
    }

    @Override
    public CC queryClosedCaptionsStatus() {
        return null;
    }

    @Override
    public Service queryServiceID() {
        return null;
    }

    @Override
    public Languages queryLanguagePreferences() {
        return null;
    }

    @Override
    public CaptionDisplay queryCaptionDisplayPreferences() {
        return null;
    }

    @Override
    public AudioAccessibilityPref queryAudioAccessibilityPreferences() {
        return null;
    }

    @Override
    public BaseURI queryReceiverWebServerURI() {
        return null;
    }

    @Override
    public List<Alerting> queryAlertingSignaling() {
        return null;
    }

    @Override
    public List<ServiceGuideUrls> queryServiceGuideURLs() {
        return null;
    }
}
