package com.GemmaGuardian.securitymonitor.data.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

object AnySerializer : KSerializer<Any> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Any")

    override fun serialize(encoder: Encoder, value: Any) {
        val jsonEncoder = encoder as? JsonEncoder ?: throw IllegalStateException("Can be used only with Json format")
        val jsonElement = serializeAny(value)
        jsonEncoder.encodeJsonElement(jsonElement)
    }

    override fun deserialize(decoder: Decoder): Any {
        val jsonDecoder = decoder as? JsonDecoder ?: throw IllegalStateException("Can be used only with Json format")
        val element = jsonDecoder.decodeJsonElement()
        return deserializeJsonElement(element)
    }

    private fun serializeAny(value: Any): JsonElement = when (value) {
        is Map<*, *> -> JsonObject(value.map { it.key.toString() to serializeAny(it.value!!) }.toMap())
        is List<*> -> JsonArray(value.map { serializeAny(it!!) })
        is Number -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        else -> JsonPrimitive(value.toString())
    }

    private fun deserializeJsonElement(element: JsonElement): Any = when (element) {
        is JsonObject -> element.map { it.key to deserializeJsonElement(it.value) }.toMap()
        is JsonArray -> element.map { deserializeJsonElement(it) }
        is JsonPrimitive -> when {
            element.isString -> element.content
            element.booleanOrNull != null -> element.boolean
            element.intOrNull != null -> element.int
            element.longOrNull != null -> element.long
            element.doubleOrNull != null -> element.double
            else -> element.content
        }
    }
}
