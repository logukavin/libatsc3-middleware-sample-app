package com.nextgenbroadcast.mobile.middleware.analytics.serializer

import android.location.Location
import com.google.gson.*
import com.nextgenbroadcast.mobile.core.toDegrees
import java.lang.reflect.Type

class LocationSerializer : JsonSerializer<Location> {
    override fun serialize(src: Location?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return src?.let {
            JsonObject().apply {
                addProperty("latitude", src.latitude.toDegrees())
                addProperty("longitude", src.longitude.toDegrees())
            }
        } ?: JsonNull.INSTANCE
    }
}