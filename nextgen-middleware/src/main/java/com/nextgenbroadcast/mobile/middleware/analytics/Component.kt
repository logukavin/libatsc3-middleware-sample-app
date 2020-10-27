package com.nextgenbroadcast.mobile.middleware.analytics

class Component(
        var componentType: Int?,
        var componentRole: Int?,
        var componentName: String?,
        var componentID: String?,
        var componentLang: String?,
        var startTime: Long?,
        var endTime: Long?,
        var sourceDeliveryPath: SourceDeliveryPath?
) {

    companion object {
        const val COMPONENT_AUDIO = 0
        const val COMPONENT_VIDEO = 1
        const val COMPONENT_CLOSED = 2
        const val COMPONENT_APPLICATION = 3
        //4 to 255 – Reserved

        //0 = Primary video, 1-254 = reserved, 255 = unknown.
    }

}