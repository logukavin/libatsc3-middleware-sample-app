package org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model

import org.ngbp.jsonrpc4jtestharness.rpc.RpcResponse

data class Languages(
        var preferredAudioLang: String? = null,
        var preferredUiLang: String? = null,
        var preferredCaptionSubtitleLang: String? = null
) : RpcResponse()