package com.thevoidkeeper.skflipper.data.services


import com.google.gson.JsonObject
import com.thevoidkeeper.skflipper.data.core.CacheManager
import com.thevoidkeeper.skflipper.data.core.HypixelApiClient
import com.thevoidkeeper.skflipper.data.util.Log
import java.time.Instant

/**
 * SKFlipper ItemInfoService
 * Version: 1.0.0
 * Compatible with: Minecraft 1.21.5 (Fabric), Kotlin 2.1.20
 *
 * Fetches and caches SkyBlock item metadata from Hypixel resources API:
 *  - Display name
 *  - Rarity / tier
 *  - Texture URL (for skull-type items)
 *  - NPC sell price (for fallback value)
 *
 * Cached under: /config/SKFlipper/cache/items_info.json
 */
object ItemInfoService {

    private const val CACHE_FILE = "items_info.json"
    private const val ENDPOINT = "https://api.hypixel.net/resources/skyblock/items"
    private var lastUpdated: Instant? = null
    private val itemMap = mutableMapOf<String, ItemInfo>()

    data class ItemInfo(
        val id: String,
        val displayName: String,
        val tier: String,
        val textureUrl: String?,
        val npcSellPrice: Double
    )

    /**
     * Loads the cached item data if available, otherwise fetches it from the API.
     * Automatically refreshes if cache is older than 24 hours.
     */
    fun load(forceRefresh: Boolean = false): Map<String, ItemInfo> {
        if (itemMap.isNotEmpty() && !forceRefresh) return itemMap

        val cache = CacheManager.readJsonObject(CACHE_FILE)
        val cacheAgeOk = cache != null && cache["lastUpdated"]?.asString?.let {
            try {
                Instant.parse(it).isAfter(Instant.now().minusSeconds(86_400))
            } catch (_: Exception) {
                false
            }
        } ?: false

        if (!forceRefresh && cacheAgeOk) {
            Log.info("Loaded ItemInfo cache from local file")
            loadFromCache(cache!!)
            return itemMap
        }

        Log.info("Fetching ItemInfo data from Hypixel API…")
        val json = HypixelApiClient.getJson(ENDPOINT, attachKey = false)
        if (json == null || !json.has("items")) {
            Log.warn("Failed to fetch item info, using cache if available")
            if (cache != null) loadFromCache(cache)
            return itemMap
        }

        parseAndCache(json)
        return itemMap
    }

    private fun loadFromCache(json: JsonObject) {
        try {
            val itemsJson = json["items"]?.asJsonArray ?: return
            itemsJson.forEach { el ->
                val obj = el.asJsonObject
                val id = obj["id"]?.asString ?: return@forEach
                itemMap[id] = ItemInfo(
                    id = id,
                    displayName = obj["displayName"]?.asString ?: id,
                    tier = obj["tier"]?.asString ?: "COMMON",
                    textureUrl = obj["textureUrl"]?.asString,
                    npcSellPrice = obj["npcSellPrice"]?.asDouble ?: 0.0
                )
            }
            lastUpdated = Instant.parse(json["lastUpdated"]?.asString ?: Instant.now().toString())
            Log.info("ItemInfo cache loaded (${itemMap.size} entries)")
        } catch (e: Exception) {
            Log.error("Failed to parse cached item info: ${e.message}")
        }
    }

    private fun parseAndCache(json: JsonObject) {
        try {
            val itemsArr = json["items"]?.asJsonArray ?: return
            itemMap.clear()

            for (el in itemsArr) {
                val obj = el.asJsonObject
                val id = obj["id"]?.asString ?: continue
                val name = obj["name"]?.asString ?: id
                val tier = obj["tier"]?.asString ?: "COMMON"

                // Extract texture URL from "texture" field if available
                val texture = obj["texture"]?.asString

                val npcSell = obj["npc_sell_price"]?.asDouble ?: 0.0

                itemMap[id] = ItemInfo(
                    id = id,
                    displayName = name,
                    tier = tier,
                    textureUrl = texture,
                    npcSellPrice = npcSell
                )
            }

            val output = JsonObject().apply {
                addProperty("lastUpdated", Instant.now().toString())
                val arr = com.google.gson.JsonArray()
                itemMap.values.forEach {
                    val entry = JsonObject()
                    entry.addProperty("id", it.id)
                    entry.addProperty("displayName", it.displayName)
                    entry.addProperty("tier", it.tier)
                    entry.addProperty("textureUrl", it.textureUrl)
                    entry.addProperty("npcSellPrice", it.npcSellPrice)
                    arr.add(entry)
                }
                add("items", arr)
            }

            CacheManager.writeJson(CACHE_FILE, output)
            Log.info("Fetched and cached ${itemMap.size} items.")
        } catch (e: Exception) {
            Log.error("Error parsing item info: ${e.message}")
        }
    }

    fun get(id: String): ItemInfo? = itemMap[id]
}
