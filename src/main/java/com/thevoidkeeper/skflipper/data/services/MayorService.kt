package com.thevoidkeeper.skflipper.data.services


import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.thevoidkeeper.skflipper.data.core.CacheManager
import com.thevoidkeeper.skflipper.data.core.HypixelApiClient
import com.thevoidkeeper.skflipper.data.util.Log
import java.time.Instant

/**
 * SKFlipper MayorService
 * Version: 1.0.0
 * Compatible with: Minecraft 1.21.5 (Fabric), Kotlin 2.1.20
 *
 * Fetches and caches SkyBlock election & active mayor data.
 * Adjusts economy constants dynamically based on perks.
 *
 * Cached under: /config/SKFlipper/cache/mayor_perks.json
 */
object MayorService {

    private const val CACHE_FILE = "mayor_perks.json"
    private const val ENDPOINT = "https://api.hypixel.net/resources/skyblock/election"

    private var lastUpdated: Instant? = null
    private var currentMayor: MayorInfo? = null

    data class MayorInfo(
        val name: String,
        val perks: List<MayorPerk>,
        val lastUpdated: Instant
    )

    data class MayorPerk(
        val name: String,
        val description: String
    )

    fun load(forceRefresh: Boolean = false): MayorInfo? {
        if (currentMayor != null && !forceRefresh) return currentMayor

        val cache = CacheManager.readJsonObject(CACHE_FILE)
        val cacheAgeOk = cache != null && cache["lastUpdated"]?.asString?.let {
            try { Instant.parse(it).isAfter(Instant.now().minusSeconds(86_400)) } catch (_: Exception) { false }
        } ?: false

        if (!forceRefresh && cacheAgeOk) {
            Log.info("Loaded Mayor cache from local file")
            currentMayor = parseFromCache(cache!!)
            return currentMayor
        }

        Log.info("Fetching active Mayor data from Hypixel API…")
        val json = HypixelApiClient.getJson(ENDPOINT, attachKey = false)
        if (json == null || !json.has("mayor")) {
            Log.warn("Failed to fetch Mayor data, using cache if available")
            if (cache != null) currentMayor = parseFromCache(cache)
            return currentMayor
        }

        val mayorJson = json["mayor"].asJsonObject
        val name = mayorJson["name"]?.asString ?: "Unknown"
        val perksArr = mayorJson["perks"]?.asJsonArray ?: JsonArray()
        val perks = mutableListOf<MayorPerk>()
        for (el in perksArr) {
            val perk = el.asJsonObject
            perks += MayorPerk(
                name = perk["name"]?.asString ?: "Unknown",
                description = perk["description"]?.asString ?: ""
            )
        }

        val info = MayorInfo(name, perks, Instant.now())
        currentMayor = info

        // Apply potential perk effects to economy constants
        applyPerksToEconomy(info)

        val output = JsonObject().apply {
            addProperty("lastUpdated", info.lastUpdated.toString())
            addProperty("mayorName", info.name)
            val arr = JsonArray()
            info.perks.forEach {
                val o = JsonObject()
                o.addProperty("name", it.name)
                o.addProperty("description", it.description)
                arr.add(o)
            }
            add("perks", arr)
        }

        CacheManager.writeJson(CACHE_FILE, output)
        Log.info("Fetched and cached active Mayor: ${info.name} (${info.perks.size} perks)")

        return info
    }

    private fun parseFromCache(json: JsonObject): MayorInfo {
        val name = json["mayorName"]?.asString ?: "Unknown"
        val perks = mutableListOf<MayorPerk>()
        json["perks"]?.asJsonArray?.forEach { el ->
            val o = el.asJsonObject
            perks += MayorPerk(
                name = o["name"]?.asString ?: "Unknown",
                description = o["description"]?.asString ?: ""
            )
        }
        val updated = Instant.parse(json["lastUpdated"]?.asString ?: Instant.now().toString())
        Log.info("Mayor cache loaded: $name (${perks.size} perks)")
        val info = MayorInfo(name, perks, updated)
        applyPerksToEconomy(info)
        return info
    }

    private fun applyPerksToEconomy(mayor: MayorInfo) {
        try {
            val economy = EconomyConstants.load()
            var adjusted = economy

            // Example known perks affecting economy
            for (perk in mayor.perks) {
                when (perk.name.lowercase()) {
                    "financial aid" -> {
                        adjusted = adjusted.copy(bazaarTax = economy.bazaarTax * 0.75)
                        Log.info("Applied Mayor perk: Financial Aid (-25% Bazaar tax)")
                    }
                    "extraordinary savings" -> {
                        adjusted = adjusted.copy(ahTax = economy.ahTax * 0.9)
                        Log.info("Applied Mayor perk: Extraordinary Savings (-10% AH tax)")
                    }
                    "boosted bits" -> {
                        adjusted = adjusted.copy(cookieBoostCoinMultiplier = economy.cookieBoostCoinMultiplier * 1.2)
                        Log.info("Applied Mayor perk: Boosted Bits (+20% Bits output)")
                    }
                }
            }

            EconomyConstants.save(adjusted)
        } catch (e: Exception) {
            Log.error("Error applying mayor perks to economy: ${e.message}")
        }
    }

    fun getCurrent(): MayorInfo? = currentMayor
}
