package org.ngbp.jsonrpc4jtestharness.rpc.queryDeviceInf.model

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DeviceInfo (
        var deviceMake: String? = null,
        var deviceModel: String? = null,
        var deviceInput: DeviceInput? = null,
        var deviceInfo: Info? = null
)

data class DeviceInput (
    var ArrowUp: Int? = null,
    var ArrowDown: Int? = null,
    var ArrowRight: Int? = null,
    var ArrowLeft: Int? = null,
    var Select: Int? = null,
    var Back: Int? = null
)

data class Info (
    var numberOfTuners: Int? = null,
    var yearOfMfr: Int? = null
)