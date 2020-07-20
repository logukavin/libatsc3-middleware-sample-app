package org.ngbp.jsonrpc4jtestharness.rpc.notification.model

data class LanguagePreferenceChangeNotification (
    var msgType: String? = null,
    var preferredAudioLang: String? = null,
    var preferredUiLang: String? = null,
    var preferredCaptionSubtitleLang: String? = null
)