package com.nextgenbroadcast.mobile.middleware.atsc3

enum class SignalingDataType {
    ALL, SLT, HELD
}

sealed interface ISignalingData {
    val type: SignalingDataType
    val name: String
    val version: Int
    val xml: String
}

sealed class Atsc3LLSTable(
    _type: SignalingDataType
) : ISignalingData {
    abstract val id: Int
    abstract val groupId: Int

    override val name: String
        get() = id.toString()
    override val type = _type
}

sealed class Atsc3LLSMetadata(
    _type: SignalingDataType
) : ISignalingData {
    override val name = _type.name
    override val type = _type
}

data class Atsc3SLTData(
    override val id: Int,
    override val version: Int,
    override val xml: String,
    override val groupId: Int
) : Atsc3LLSTable(SignalingDataType.SLT)

data class Atsc3HELDData(
    override val version: Int,
    override val xml: String
) : Atsc3LLSMetadata(SignalingDataType.HELD)
