package org.ngbp.libatsc3.middleware.android.phy;

// Stub for SaankhyaPHYAndroid from libatsc3/atsc3_phy_saankhya lib
public class SaankhyaPHYAndroid {

    //jjustman-2021-04-28 - <1204, 243> is the generic VID/PID for the FX3 bootloader
    public static final int CYPRESS_VENDOR_ID = 1204;
    public static final int FX3_PREBOOT_PRODUCT_ID = 243;

    //this PID is only re-enumerated after our FX3 preboot firmware has been loaded based upon DeviceTypeSelectionDialog disambiguation
    public static final int KAILASH_OR_YOGA_PRODUCT_ID = 240;
    public static final String KAILASH_FIRMWARE_MFG_NAME_JJ = "JJ5ress";

    // The same values as in SaankhyaPHYAndroid.h
    public static final int DEVICE_TYPE_MARKONE = 0;
    public static final int DEVICE_TYPE_FX3_KAILASH = 1;
    public static final int DEVICE_TYPE_FX3_YOGA = 3;
}