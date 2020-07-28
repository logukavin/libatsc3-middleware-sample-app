package org.ngbp.jsonrpc4jtestharness.rpc.processor

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreType
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.nmuzhichin.jsonrpc.module.JsonRpcModule

class RPCObjectMapperUtils {
    @JsonIgnoreType
    private class MixInForIgnoreType

    private val objectMapper = ObjectMapper().apply {
        setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
        addMixIn(Throwable::class.java, MixInForIgnoreType::class.java)
        registerModule(JsonRpcModule())
    }

    fun <T> jsonToObject(json: String, clazz: Class<T>): T = objectMapper.readValue(json, clazz)

    fun <T> objectToJson(any: T): String = objectMapper.writeValueAsString(any)

    fun <T> objectToMap(any: T): Map<*, *> = objectMapper.convertValue(any, Map::class.java)
}
