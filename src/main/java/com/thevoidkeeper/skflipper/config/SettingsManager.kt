package com.thevoidkeeper.skflipper.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path

object SettingsManager {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val configDir: Path = FabricLoader.getInstance().configDir.resolve("SKFlipper")
    private val file: Path = configDir.resolve("settings.json")

    @Volatile
    var settings: Settings = Settings()
        private set

    fun load() {
        try {
            Files.createDirectories(configDir)
            if (Files.exists(file)) {
                Files.newBufferedReader(file).use {
                    settings = gson.fromJson(it, Settings::class.java) ?: Settings()
                }
            } else save()
        } catch (e: Exception) {
            settings = Settings()
        }
    }

    fun save() {
        try {
            Files.createDirectories(configDir)
            Files.newBufferedWriter(file).use {
                gson.toJson(settings, it)
            }
        } catch (_: Exception) {}
    }

    fun update(mutator: (Settings) -> Unit) {
        mutator(settings)
        save()
    }
}
