package com.thevoidkeeper.skflipper.data.core

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.thevoidkeeper.skflipper.data.util.Log
import net.fabricmc.loader.api.FabricLoader
import java.io.File
import java.io.FileWriter
import java.time.Instant

/**
 * SKFlipper CacheManager
 * Version: 1.2.0
 * Compatible with: Minecraft 1.21.5 (Fabric), Kotlin 2.1.20
 *
 * Handles read/write for cached JSON and history data.
 */
object CacheManager {

    private val gson = Gson()
    private val baseDir = File(FabricLoader.getInstance().configDir.toFile(), "SKFlipper/cache")
    private val debugDir = File(FabricLoader.getInstance().configDir.toFile(), "SKFlipper/debug")

    init {
        if (!baseDir.exists()) baseDir.mkdirs()
        if (!debugDir.exists()) debugDir.mkdirs()
    }

    /** Reads a JsonObject from cache file (returns null if invalid or missing). */
    fun readJsonObject(filename: String): JsonObject? {
        return try {
            val file = File(baseDir, filename)
            if (!file.exists()) return null
            val content = file.readText()
            gson.fromJson(content, JsonObject::class.java)
        } catch (e: Exception) {
            Log.error("Cache read error for $filename: ${e.message}")
            null
        }
    }

    /** Writes a JsonObject to cache file. */
    fun writeJson(filename: String, json: JsonObject) {
        try {
            val file = File(baseDir, filename)
            FileWriter(file, false).use { writer ->
                gson.toJson(json, writer)
            }
        } catch (e: Exception) {
            Log.error("Cache write error for $filename: ${e.message}")
        }
    }

    /** Appends a JsonObject as one line to a .jsonl history file. */
    fun appendHistory(filename: String, json: JsonObject) {
        try {
            val file = File(baseDir, filename)
            FileWriter(file, true).use { writer ->
                gson.toJson(json, writer)
                writer.appendLine()
            }
        } catch (e: Exception) {
            Log.error("History append error for $filename: ${e.message}")
        }
    }

    /** Writes a raw debug file (for malformed API responses, etc.). */
    fun writeDebugDump(filename: String, content: String) {
        try {
            val file = File(debugDir, "${Instant.now().epochSecond}_$filename")
            file.writeText(content)
            Log.warn("Debug dump written: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.error("Failed to write debug dump: ${e.message}")
        }
    }

    /** Returns the absolute cache directory (useful for diagnostics). */
    fun cacheDir(): File = baseDir

    /** Ensures folder structure exists (used in DataIntegrity). */
    fun ensureCacheStructure() {
        try {
            if (!baseDir.exists()) baseDir.mkdirs()
            val logs = File(FabricLoader.getInstance().configDir.toFile(), "SKFlipper/logs")
            if (!logs.exists()) logs.mkdirs()
            Log.info("Cache structure verified at ${baseDir.absolutePath}")
        } catch (e: Exception) {
            Log.error("Failed to ensure cache structure: ${e.message}")
        }
    }
}
