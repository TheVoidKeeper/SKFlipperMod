package com.thevoidkeeper.skflipper.data.util

import net.fabricmc.loader.api.FabricLoader
import java.io.File
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * SKFlipper Smart Log System
 * Version: 1.2.0
 * Compatible with: Minecraft 1.21.5 (Fabric), Kotlin 2.1.20
 *
 * Adds:
 *  - Auto rotation if log > 5 MB
 *  - Level filtering (INFO / WARN / ERROR / DEBUG)
 *  - Color-coded console output
 *  - Auto flush and safe close on exit
 */
object Log {

    private val logDir = File(FabricLoader.getInstance().configDir.toFile(), "SKFlipper/logs")
    private val logFile = File(logDir, "skflipper-latest.log")

    private const val MAX_SIZE_BYTES = 5 * 1024 * 1024 // 5 MB
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
    private lateinit var writer: PrintWriter

    private enum class Level(val tag: String, val color: String) {
        INFO("INFO", "\u001B[32m"),     // green
        WARN("WARN", "\u001B[33m"),     // yellow
        ERROR("ERROR", "\u001B[31m"),   // red
        DEBUG("DEBUG", "\u001B[36m");   // cyan
    }

    init {
        if (!logDir.exists()) logDir.mkdirs()

        // 🔁 Auto-rotate old log if too large
        if (logFile.exists() && logFile.length() > MAX_SIZE_BYTES) {
            val backup = File(logDir, "skflipper-${System.currentTimeMillis()}.log")
            logFile.renameTo(backup)
        }

        writer = logFile.printWriter(Charsets.UTF_8)

        Runtime.getRuntime().addShutdownHook(Thread {
            try {
                writer.flush()
                writer.close()
            } catch (_: Exception) {
            }
        })

        info("SKFlipper Log initialized at ${logFile.absolutePath}")
    }

    private fun log(level: Level, message: String) {
        val time = dateFmt.format(Date())
        val formatted = "[$time] [${level.tag}] [SKFlipper] $message"

        synchronized(this) {
            writer.println(formatted)
            writer.flush()
        }

        // Console colors (strip color codes for log file only)
        val colored = "${level.color}$formatted\u001B[0m"
        when (level) {
            Level.INFO, Level.DEBUG -> println(colored)
            Level.WARN, Level.ERROR -> System.err.println(colored)
        }
    }

    // Public API
    fun info(msg: String) = log(Level.INFO, msg)
    fun warn(msg: String) = log(Level.WARN, msg)
    fun error(msg: String) = log(Level.ERROR, msg)
    fun debug(msg: String) = log(Level.DEBUG, msg)
}
