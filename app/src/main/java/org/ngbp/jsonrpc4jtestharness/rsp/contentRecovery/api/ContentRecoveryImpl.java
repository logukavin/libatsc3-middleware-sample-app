package org.ngbp.jsonrpc4jtestharness.rsp.contentRecovery.api;

import org.ngbp.jsonrpc4jtestharness.rsp.contentRecovery.model.CecoveredComponentInfo;
import org.ngbp.jsonrpc4jtestharness.rsp.contentRecovery.model.ContentRecoveryState;

public class ContentRecoveryImpl implements ContentRecovery {

    @Override
    public ContentRecoveryState queryContentRecoveryState() {
        return null;
    }

    @Override
    public CecoveredComponentInfo queryRecoveredComponentInfo() {
        return null;
    }

    @Override
    public CecoveredComponentInfo contentRecoveryStateChangeNotification() {
        return null;
    }
}
