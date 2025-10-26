package com.thevoidkeeper.skflipper.data.services


import com.google.gson.JsonObject
import com.thevoidkeeper.skflipper.data.core.HypixelApiClient
import com.thevoidkeeper.skflipper.data.core.CacheManager
import com.thevoidkeeper.skflipper.data.util.Log
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

object BazaarService {

    private const val CACHE_FILE = "bazaar_snapshot.json"
    private const val HISTORY_FILE = "bazaar_history.jsonl"
    private const val ENDPOINT = "https://api.hypixel.net/skyblock/bazaar"
    private const val REFRESH_INTERVAL_SECONDS = 10L

    private val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "SKFlipper-Bazaar-Refresh").apply { isDaemon = true }
    }

    private val lastSnapshot = AtomicReference<Snapshot?>()
    private var running = false

    data class Product(
        val id: String,
        val name: String,
        val sellPrice: Double,
        val buyPrice: Double,
        val sellVolume: Long,
        val buyVolume: Long
    )

    data class Snapshot(
        val fetchedAt: Instant,
        val products: Map<String, Product>
    )

    fun startAutoRefresh() {
        if (running) return
        running = true
        Log.info("Bazaar auto-refresh loop started ($REFRESH_INTERVAL_SECONDS s)")
        scheduler.scheduleAtFixedRate(
            { refreshNow() },
            0, REFRESH_INTERVAL_SECONDS, TimeUnit.SECONDS
        )
    }

    fun currentSnapshot(): Snapshot? = lastSnapshot.get()

    fun refreshNow() {
        try {
            val json = HypixelApiClient.getJson(ENDPOINT)
            if (json == null || !json.has("products")) {
                Log.warn("Bazaar fetch failed → using cache")
                val cache = CacheManager.readJsonObject(CACHE_FILE)
                if (cache != null) lastSnapshot.set(parseCache(cache))
                return
            }

            val map = mutableMapOf<String, Product>()
            val productsJson = json["products"].asJsonObject
            for ((id, entry) in productsJson.entrySet()) {
                val quick = entry.asJsonObject["quick_status"]?.asJsonObject ?: continue
                map[id] = Product(
                    id,
                    id,
                    quick["sellPrice"]?.asDouble ?: 0.0,
                    quick["buyPrice"]?.asDouble ?: 0.0,
                    quick["sellVolume"]?.asLong ?: 0,
                    quick["buyVolume"]?.asLong ?: 0
                )
            }

            val snapshot = Snapshot(Instant.now(), map)
            lastSnapshot.set(snapshot)

            CacheManager.writeJson(CACHE_FILE, snapshotToJson(snapshot))
            CacheManager.appendHistory(HISTORY_FILE, snapshotToJson(snapshot))

            Log.info("Bazaar snapshot fetched (${map.size} items).")

        } catch (e: Exception) {
            Log.error("Bazaar refresh error: ${e.message}")
        }
    }

    private fun snapshotToJson(snapshot: Snapshot): JsonObject {
        val json = JsonObject()
        json.addProperty("fetchedAt", snapshot.fetchedAt.toString())
        val productsJson = JsonObject()
        snapshot.products.values.forEach {
            val obj = JsonObject()
            obj.addProperty("id", it.id)
            obj.addProperty("name", it.name)
            obj.addProperty("sellPrice", it.sellPrice)
            obj.addProperty("buyPrice", it.buyPrice)
            obj.addProperty("sellVolume", it.sellVolume)
            obj.addProperty("buyVolume", it.buyVolume)
            productsJson.add(it.id, obj)
        }
        json.add("products", productsJson)
        return json
    }

    private fun parseCache(json: JsonObject): Snapshot {
        val products = mutableMapOf<String, Product>()
        val productsJson = json["products"].asJsonObject
        for ((id, entry) in productsJson.entrySet()) {
            val obj = entry.asJsonObject
            products[id] = Product(
                id,
                obj["name"]?.asString ?: id,
                obj["sellPrice"]?.asDouble ?: 0.0,
                obj["buyPrice"]?.asDouble ?: 0.0,
                obj["sellVolume"]?.asLong ?: 0,
                obj["buyVolume"]?.asLong ?: 0
            )
        }
        return Snapshot(Instant.now(), products)
    }
}
