package org.ngbp.jsonrpc4jtestharness.rsp.receiverQueryApi.api;

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

public class ReceiverQueryApiImpl implements ReceiverQueryApi {
    @Override
    public JsonRpcResponse<RatingLevel> queryContentAdvisoryRating() {
        return null;
    }

    @Override
    public JsonRpcResponse<CC> queryClosedCaptionsStatus() {
        return null;
    }

    @Override
    public JsonRpcResponse<Service> queryServiceID() {
        return null;
    }

    @Override
    public JsonRpcResponse<Languages> queryLanguagePreferences() {
        return null;
    }

    @Override
    public JsonRpcResponse<CaptionDisplay> queryCaptionDisplayPreferences() {
        return null;
    }

    @Override
    public JsonRpcResponse<AudioAccessibilityPref> queryAudioAccessibilityPreferences() {
        return null;
    }

    @Override
    public JsonRpcResponse<BaseURI> queryReceiverWebServerURI() {
        return null;
    }

    @Override
    public JsonRpcResponse<List<Alerting>> queryAlertingSignaling() {
        return null;
    }

    @Override
    public JsonRpcResponse<List<ServiceGuideUrls>> queryServiceGuideURLs() {
        return null;
    }
}
