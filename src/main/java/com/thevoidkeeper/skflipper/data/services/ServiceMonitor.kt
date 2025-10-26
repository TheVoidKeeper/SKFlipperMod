package com.thevoidkeeper.skflipper.data.services


import com.thevoidkeeper.skflipper.data.core.ApiKeyCycler
import com.thevoidkeeper.skflipper.data.util.Log
import java.lang.management.ManagementFactory
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * SKFlipper ServiceMonitor
 * Version: 1.0.0
 * Compatible with: Minecraft 1.21.5 (Fabric), Kotlin 2.1.20
 *
 * Monitors all background services for thread health and API usage.
 * Writes periodic summaries to the SKFlipper log.
 */
object ServiceMonitor {

    private val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "SKFlipper-ServiceMonitor").apply { isDaemon = true }
    }

    private var startTime: Instant = Instant.now()
    private var lastBazaarFetch = 0L
    private var lastAhFetch = 0L

    /**
     * Starts periodic status logging (every 60 seconds).
     * Safe to call multiple times.
     */
    fun start() {
        Log.info("Starting SKFlipper Service Monitor…")

        scheduler.scheduleAtFixedRate({
            try {
                val uptime = (System.currentTimeMillis() - startTime.toEpochMilli()) / 1000
                val threads = ManagementFactory.getThreadMXBean().threadCount
                val apiKeys = ApiKeyCycler.hasKeys()
                val bazaarSnap = BazaarService.currentSnapshot()
                val ahSnap = AuctionHouseService.currentSnapshot()

                if (bazaarSnap != null) lastBazaarFetch = bazaarSnap.fetchedAt.epochSecond
                if (ahSnap != null) lastAhFetch = ahSnap.fetchedAt.epochSecond

                val now = Instant.now().epochSecond
                val bazaarAge = now - lastBazaarFetch
                val ahAge = now - lastAhFetch

                val bazaarStatus = if (bazaarSnap != null) "✅ ${bazaarSnap.products.size} items" else "❌ none"
                val ahStatus = if (ahSnap != null) "✅ ${ahSnap.auctions.size} auctions" else "❌ none"

                Log.info(
                    """
                    === SKFlipper Service Monitor ===
                    Uptime: ${uptime}s | Threads: $threads | API Keys: ${if (apiKeys) "Active" else "Missing"}
                    Bazaar: $bazaarStatus (age ${bazaarAge}s)
                    AH: $ahStatus (age ${ahAge}s)
                    ================================
                    """.trimIndent()
                )

            } catch (e: Exception) {
                Log.error("ServiceMonitor error: ${e.message}")
            }
        }, 15, 60, TimeUnit.SECONDS)
    }
}
