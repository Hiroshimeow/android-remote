package com.example.protocol

import kotlinx.serialization.json.Json

val protocolJson = Json { 
    ignoreUnknownKeys = true 
    encodeDefaults = true 
}
