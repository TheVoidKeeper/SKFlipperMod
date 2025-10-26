package com.thevoidkeeper.skflipper.data.core

import com.thevoidkeeper.skflipper.data.services.*
import com.thevoidkeeper.skflipper.data.util.Log

/**
 * SKFlipper DataIntegrity
 * ------------------------
 * Initialisiert beim Modstart alle Datenebenen, überprüft Cache-Struktur,
 * lädt API-Keys, startet Auto-Refresh für Bazaar und AH.
 *
 * Wird von SKFlipperMod.onInitializeClient() aufgerufen.
 */
object DataIntegrity {

    private var validated = false

    /**
     * Führt vollständigen Integritäts-Check und Initialisierung aller Services durch.
     * Gibt true zurück, wenn alles korrekt initialisiert wurde.
     */
    fun validateALL(): Boolean {
        if (validated) return true

        Log.info("Running SKFlipper DataIntegrity checks...")

        return try {
            // ── 1️⃣ Cache-Struktur prüfen / anlegen ────────────────────────────
            CacheManager.ensureCacheStructure()

            // ── 2️⃣ Hypixel API vorbereiten (Keys laden, HTTP-Client) ──────────
            HypixelApiClient.initialize()

            // ── 3️⃣ Statische Daten laden ──────────────────────────────────────
            EconomyConstants.load()
            ItemInfoService.load(false)
            BitsShopService.load(false)
            MayorService.load(false)
            CalendarService.load(false)

            // ── 4️⃣ Live-Systeme initialisieren ─────────────────────────────────
            BazaarService.refreshNow()
            BazaarService.startAutoRefresh()

            AuctionHouseService.refreshNow()
            AuctionHouseService.startAutoRefresh()

            // ── 5️⃣ Monitoring aktivieren ──────────────────────────────────────
            ServiceMonitor.start()

            validated = true
            Log.info("✅ SKFlipper DataIntegrity completed – all systems initialized successfully.")
            true
        } catch (e: Exception) {
            Log.error("❌ DataIntegrity failed: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}
