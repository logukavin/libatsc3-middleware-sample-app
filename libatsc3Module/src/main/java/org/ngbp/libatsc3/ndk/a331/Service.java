package org.ngbp.libatsc3.ndk.a331;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Service {
    int serviceId;
    String globalServiceId;
    int majorChannelNo;
    int minorChannelNo;
    int serviceCategory;

    String shortServiceName;

    List<BroadcastSvcSignaling> broadcastSvcSignalingCollection = new ArrayList<>();

    public int getServiceId() {
        return serviceId;
    }

    public String getGlobalServiceId() {
        return globalServiceId;
    }

    public int getMajorChannelNo() {
        return majorChannelNo;
    }

    public int getMinorChannelNo() {
        return minorChannelNo;
    }

    public int getServiceCategory() {
        return serviceCategory;
    }

    public String getShortServiceName() {
        return shortServiceName;
    }

    public List<BroadcastSvcSignaling> getBroadcastSvcSignalingCollection() {
        return Collections.unmodifiableList(broadcastSvcSignalingCollection);
    }

    @NonNull
    public String toString() {
        return String.format(Locale.US, "%d.%d %s", majorChannelNo, minorChannelNo, shortServiceName);
    }
}
