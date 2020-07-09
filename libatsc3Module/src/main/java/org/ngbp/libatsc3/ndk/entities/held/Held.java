package org.ngbp.libatsc3.ndk.entities.held;

public class Held {
    String appContextId;
    String bcastEntryPackageUrl;
    String bcastEntryPageUrl;
    int coupledServices;
    boolean appRendering;

    public String getAppContextId() {
        return appContextId;
    }

    public String getBcastEntryPackageUrl() {
        return bcastEntryPackageUrl;
    }

    public String getBcastEntryPageUrl() {
        return bcastEntryPageUrl;
    }

    public int getCoupledServices() {
        return coupledServices;
    }

    public boolean isAppRendering() {
        return appRendering;
    }
}
