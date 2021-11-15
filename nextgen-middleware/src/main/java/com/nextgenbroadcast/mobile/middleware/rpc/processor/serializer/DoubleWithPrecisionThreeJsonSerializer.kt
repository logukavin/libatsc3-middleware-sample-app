package com.nextgenbroadcast.mobile.middleware.rpc.processor.serializer

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

class DoubleWithPrecisionThreeJsonSerializer : JsonSerializer<Double>() {
    override fun serialize(value: Double, gen: JsonGenerator, serializers: SerializerProvider?) {
        gen.writeNumber(String.format("%.3f", value))
    }
}