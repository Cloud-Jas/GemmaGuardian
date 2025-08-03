package com.GemmaGuardian.securitymonitor.data.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

object SerializationConfig {
    val json = Json {
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            contextual(AnySerializer)
        }
    }
}
