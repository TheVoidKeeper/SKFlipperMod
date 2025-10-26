package com.thevoidkeeper.skflipper.data.services


import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.thevoidkeeper.skflipper.data.core.HypixelApiClient
import com.thevoidkeeper.skflipper.data.core.CacheManager
import com.thevoidkeeper.skflipper.data.util.Log
import java.time.Instant

/**
 * SKFlipper BitsShopService
 * Version: 1.0.0
 * Compatible with: Minecraft 1.21.5 (Fabric), Kotlin 2.1.20
 *
 * Fetches and caches Hypixel SkyBlock Bits Shop data.
 * Since the official endpoint is limited, this class also supports
 * offline/local loading for manual extension.
 *
 * Cached under: /config/SKFlipper/cache/bits_shop.json
 */
object BitsShopService {

    private const val CACHE_FILE = "bits_shop.json"
    private const val ENDPOINT = "https://api.hypixel.net/resources/skyblock/bits_shop"

    private var lastUpdated: Instant? = null
    private val items = mutableMapOf<String, BitsItem>()

    data class BitsItem(
        val id: String,
        val name: String,
        val bitsCost: Int,
        val category: String,
        val description: String?,
        val rarity: String?,
        val discount: Double = 0.0
    )

    fun load(forceRefresh: Boolean = false): Map<String, BitsItem> {
        if (items.isNotEmpty() && !forceRefresh) return items

        val cache = CacheManager.readJsonObject(CACHE_FILE)
        val cacheAgeOk = cache != null && cache["lastUpdated"]?.asString?.let {
            try { Instant.parse(it).isAfter(Instant.now().minusSeconds(86_400)) } catch (_: Exception) { false }
        } ?: false

        if (!forceRefresh && cacheAgeOk) {
            Log.info("Loaded Bits Shop cache from local file")
            loadFromCache(cache!!)
            return items
        }

        Log.info("Fetching Bits Shop data from Hypixel API…")
        val json = HypixelApiClient.getJson(ENDPOINT, attachKey = false)
        if (json == null || !json.has("shop_items")) {
            Log.warn("Failed to fetch Bits Shop data, using cache if available")
            if (cache != null) loadFromCache(cache)
            return items
        }

        parseAndCache(json)
        return items
    }

    private fun loadFromCache(json: JsonObject) {
        try {
            val arr = json["items"]?.asJsonArray ?: return
            arr.forEach { el ->
                val o = el.asJsonObject
                val id = o["id"]?.asString ?: return@forEach
                items[id] = BitsItem(
                    id = id,
                    name = o["name"]?.asString ?: id,
                    bitsCost = o["bitsCost"]?.asInt ?: 0,
                    category = o["category"]?.asString ?: "misc",
                    description = o["description"]?.asString,
                    rarity = o["rarity"]?.asString,
                    discount = o["discount"]?.asDouble ?: 0.0
                )
            }
            lastUpdated = Instant.parse(json["lastUpdated"]?.asString ?: Instant.now().toString())
            Log.info("Bits Shop cache loaded (${items.size} entries)")
        } catch (e: Exception) {
            Log.error("Failed to parse cached Bits Shop: ${e.message}")
        }
    }

    private fun parseAndCache(json: JsonObject) {
        try {
            val arr = json["shop_items"]?.asJsonArray ?: JsonArray()
            items.clear()

            for (el in arr) {
                val obj = el.asJsonObject
                val id = obj["id"]?.asString ?: continue
                items[id] = BitsItem(
                    id = id,
                    name = obj["name"]?.asString ?: id,
                    bitsCost = obj["cost"]?.asInt ?: 0,
                    category = obj["category"]?.asString ?: "misc",
                    description = obj["description"]?.asString,
                    rarity = obj["tier"]?.asString,
                    discount = 0.0
                )
            }

            val output = JsonObject().apply {
                addProperty("lastUpdated", Instant.now().toString())
                val arrOut = JsonArray()
                items.values.forEach {
                    val o = JsonObject()
                    o.addProperty("id", it.id)
                    o.addProperty("name", it.name)
                    o.addProperty("bitsCost", it.bitsCost)
                    o.addProperty("category", it.category)
                    o.addProperty("description", it.description)
                    o.addProperty("rarity", it.rarity)
                    o.addProperty("discount", it.discount)
                    arrOut.add(o)
                }
                add("items", arrOut)
            }

            CacheManager.writeJson(CACHE_FILE, output)
            Log.info("Fetched and cached ${items.size} Bits Shop entries")
        } catch (e: Exception) {
            Log.error("Error parsing Bits Shop data: ${e.message}")
        }
    }

    fun get(id: String): BitsItem? = items[id]
}
