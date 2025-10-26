package com.thevoidkeeper.skflipper.data.core

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.stream.JsonReader
import com.thevoidkeeper.skflipper.data.util.Log
import java.io.StringReader
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * SKFlipper Hypixel API Client
 * Version: 1.2.1
 * Compatible with: Minecraft 1.21.5 (Fabric), Kotlin 2.1.20
 */
object HypixelApiClient {

    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    private val gson = Gson()
    private var initialized = false

    /** Initialisiert ApiKeyCycler und prüft Keys */
    fun initialize() {
        if (initialized) return
        try {
            ApiKeyCycler.loadKeys()
            initialized = true
            Log.info("Hypixel API client initialized with ${ApiKeyCycler.keyCount()} key(s).")
        } catch (e: Exception) {
            Log.error("Failed to initialize HypixelApiClient: ${e.message}")
        }
    }

    /**
     * Führt GET-Request aus, optional mit Hypixel-API-Key.
     */
    fun getJson(baseUrl: String, params: String = "", attachKey: Boolean = true): JsonObject? {
        return try {
            val key = if (attachKey) ApiKeyCycler.nextKey() else null
            val connector = if (baseUrl.contains("?")) "&" else "?"
            val withKey = if (attachKey && key != null) "${connector}key=$key" else ""
            val withParams = if (params.isNotEmpty()) {
                if (withKey.isEmpty() && !baseUrl.contains("?")) "$connector$params" else "&$params"
            } else ""
            val fullUrl = baseUrl + withKey + withParams

            val req = HttpRequest.newBuilder()
                .uri(URI(fullUrl))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build()

            val resp = client.send(req, HttpResponse.BodyHandlers.ofString())
            if (resp.statusCode() != 200) {
                Log.warn("HTTP ${resp.statusCode()} from Hypixel → $baseUrl")
                return null
            }

            val text = resp.body()
            if (!text.trimStart().startsWith("{")) {
                Log.warn("Non-JSON response from Hypixel API: ${text.take(100)}")
                return null
            }

            val reader = JsonReader(StringReader(text))
            reader.isLenient = true
            gson.fromJson(reader, JsonObject::class.java)
        } catch (e: Exception) {
            Log.error("Hypixel request failed: ${e.message}")
            null
        }
    }

    fun isHealthy(): Boolean {
        return try {
            val ping = getJson("https://api.hypixel.net/skyblock/bazaar", attachKey = false)
            ping != null && (ping["success"]?.asBoolean == true || ping.has("products"))
        } catch (e: Exception) {
            Log.error("API health check failed: ${e.message}")
            false
        }
    }
}
