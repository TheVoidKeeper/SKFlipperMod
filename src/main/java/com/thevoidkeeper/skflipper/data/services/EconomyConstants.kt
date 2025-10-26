package com.thevoidkeeper.skflipper.data.services


import com.google.gson.JsonObject
import com.thevoidkeeper.skflipper.data.core.CacheManager
import com.thevoidkeeper.skflipper.data.util.Log
import java.time.Instant

/**
 * SKFlipper Economy Constants
 * Version: 1.0.0
 * Compatible with: Minecraft 1.21.5 (Fabric), Kotlin 2.1.20
 *
 * Central reference for static and dynamic economy values:
 * - Bazaar and Auction taxes
 * - Listing fees and duration multipliers
 * - Bits/upgrade modifiers
 * - Update + cache support
 */
data class EconomyConstants(
    val bazaarTax: Double = 0.0125,
    val ahTax: Double = 0.01,
    val ahListingFeeMultipliers: Map<Int, Double> = mapOf(
        6 to 0.01,
        12 to 0.02,
        24 to 0.03,
        48 to 0.04
    ),
    val ahMaxDurationHours: Int = 48,
    val npcSellTaxReduction: Double = 0.0,  // reserved for perks
    val cookieBoostCoinMultiplier: Double = 1.0,
    val lastUpdated: Instant = Instant.now()
) {

    fun toJson(): JsonObject = JsonObject().apply {
        addProperty("bazaarTax", bazaarTax)
        addProperty("ahTax", ahTax)
        val fees = JsonObject()
        ahListingFeeMultipliers.forEach { (h, m) -> fees.addProperty(h.toString(), m) }
        add("ahListingFeeMultipliers", fees)
        addProperty("ahMaxDurationHours", ahMaxDurationHours)
        addProperty("npcSellTaxReduction", npcSellTaxReduction)
        addProperty("cookieBoostCoinMultiplier", cookieBoostCoinMultiplier)
        addProperty("lastUpdated", lastUpdated.toString())
    }

    companion object {

        private const val CACHE_FILE = "economy_constants.json"

        fun load(): EconomyConstants {
            val json = CacheManager.readJsonObject(CACHE_FILE) ?: run {
                val defaults = EconomyConstants()
                CacheManager.writeJson(CACHE_FILE, defaults.toJson())
                Log.info("Initialized default economy constants cache")
                return defaults
            }
            return try {
                val bazaarTax = json["bazaarTax"]?.asDouble ?: 0.0125
                val ahTax = json["ahTax"]?.asDouble ?: 0.01
                val feeMap = mutableMapOf<Int, Double>()
                json["ahListingFeeMultipliers"]?.asJsonObject?.entrySet()?.forEach { (k, v) ->
                    feeMap[k.toInt()] = v.asDouble
                }
                val npcReduction = json["npcSellTaxReduction"]?.asDouble ?: 0.0
                val cookieMult = json["cookieBoostCoinMultiplier"]?.asDouble ?: 1.0
                EconomyConstants(
                    bazaarTax = bazaarTax,
                    ahTax = ahTax,
                    ahListingFeeMultipliers = feeMap,
                    npcSellTaxReduction = npcReduction,
                    cookieBoostCoinMultiplier = cookieMult
                )
            } catch (e: Exception) {
                Log.error("Failed to parse economy constants: ${e.message}")
                EconomyConstants()
            }
        }

        fun save(constants: EconomyConstants) {
            CacheManager.writeJson(CACHE_FILE, constants.toJson())
        }
    }
}
