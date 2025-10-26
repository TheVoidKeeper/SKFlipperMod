package com.thevoidkeeper.skflipper.data.core

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.thevoidkeeper.skflipper.data.util.Log
import net.fabricmc.loader.api.FabricLoader
import java.io.File
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

/**
 * SKFlipper API Key Cycler
 * Lädt Hypixel API Keys aus config/SKFlipper/api_keys.json
 * und rotiert automatisch zwischen ihnen, um Rate Limits zu verteilen.
 *
 * Beispiel api_keys.json:
 * {
 *   "keys": ["<uuid-1>", "<uuid-2>"],
 *   "maxPerMinute": 110
 * }
 */
object ApiKeyCycler {

    private val gson = Gson()
    private val configFile = File(FabricLoader.getInstance().configDir.toFile(), "SKFlipper/api_keys.json")

    private val keys: MutableList<String> = mutableListOf()
    private val currentIndex = AtomicInteger(0)
    private var maxPerMinute: Int = 110
    private var lastLoadTime: Long = 0

    /** Keys aus Datei laden (persistenter Speicher). */
    fun loadKeys() {
        try {
            if (!configFile.exists()) {
                Log.warn("No api_keys.json found – creating default config.")
                saveDefault()
                keys.clear()
                maxPerMinute = 110
                lastLoadTime = Instant.now().epochSecond
                return
            }

            val json = gson.fromJson(configFile.readText(), JsonObject::class.java)
            keys.clear()
            val arr = json.getAsJsonArray("keys") ?: JsonArray()
            arr.forEach { el -> keys.add(el.asString) }
            maxPerMinute = json.get("maxPerMinute")?.asInt ?: 110

            lastLoadTime = Instant.now().epochSecond
            Log.info("Loaded ${keys.size} API key(s) from api_keys.json (maxPerMinute=$maxPerMinute).")
        } catch (e: Exception) {
            Log.error("Failed to load API keys: ${e.message}")
        }
    }

    /** Default-Datei erzeugen, falls nicht vorhanden. */
    private fun saveDefault() {
        try {
            val dir = configFile.parentFile
            if (!dir.exists()) dir.mkdirs()

            val defaultJson = JsonObject()
            defaultJson.add("keys", JsonArray())
            defaultJson.addProperty("maxPerMinute", 110)

            configFile.writeText(gson.toJson(defaultJson))
            Log.info("Created new api_keys.json at ${configFile.absolutePath}")
        } catch (e: Exception) {
            Log.error("Failed to create default api_keys.json: ${e.message}")
        }
    }

    /** Nächsten Key in Rotation holen. */
    fun nextKey(): String? {
        if (keys.isEmpty()) {
            Log.error("No API keys loaded! Hypixel requests will fail.")
            return null
        }
        val index = currentIndex.getAndIncrement() % keys.size
        return keys[index]
    }

    /** Variante ohne Fehlerlog bei leerer Liste. */
    fun nextKeyOrNull(): String? = if (keys.isEmpty()) null else nextKey()

    /** Anzahl geladener Keys. */
    fun keyCount(): Int = keys.size

    /** Gibt true zurück, wenn mindestens ein Key geladen ist. */
    fun hasKeys(): Boolean = keys.isNotEmpty()

    /** Sekunden seit letztem Laden. */
    fun lastLoadedAgoSeconds(): Long = Instant.now().epochSecond - lastLoadTime

    /** konfiguriertes Requests/Minute-Limit. */
    fun maxRequestsPerMinute(): Int = maxPerMinute
}
