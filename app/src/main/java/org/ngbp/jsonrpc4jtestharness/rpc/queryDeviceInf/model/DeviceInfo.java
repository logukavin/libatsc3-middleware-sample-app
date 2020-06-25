package org.ngbp.jsonrpc4jtestharness.rpc.queryDeviceInf.model;

public class DeviceInfo {
    public String deviceMake;
    public String deviceModel;
    public DeviceInput deviceInput;
    public Info deviceInfo;

    class DeviceInput {
        public Integer ArrowUp;
        public Integer ArrowDown;
        public Integer ArrowRight;
        public Integer ArrowLeft;
        public Integer Select;
        public Integer Back;
    }

    class Info {
        public Integer numberOfTuners;
        public Integer yearOfMfr;
    }
}
