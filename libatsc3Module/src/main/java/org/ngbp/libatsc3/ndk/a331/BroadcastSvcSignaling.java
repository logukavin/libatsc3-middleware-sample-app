package org.ngbp.libatsc3.ndk.a331;

public class BroadcastSvcSignaling {
    int slsProtocol;
    int slsMajorProtocolVersion;
    int slsMinorProtocolVersion;
    String slsDestinationIpAddress;
    String slsDestinationUdpPort;
    String slsSourceIpAddress;

    public int getSlsProtocol() {
        return slsProtocol;
    }

    public int getSlsMajorProtocolVersion() {
        return slsMajorProtocolVersion;
    }

    public int getSlsMinorProtocolVersion() {
        return slsMinorProtocolVersion;
    }

    public String getSlsDestinationIpAddress() {
        return slsDestinationIpAddress;
    }

    public String getSlsDestinationUdpPort() {
        return slsDestinationUdpPort;
    }

    public String getSlsSourceIpAddress() {
        return slsSourceIpAddress;
    }
}
