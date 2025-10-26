package com.thevoidkeeper.skflipper.data.services

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.thevoidkeeper.skflipper.data.core.HypixelApiClient
import com.thevoidkeeper.skflipper.data.core.CacheManager
import com.thevoidkeeper.skflipper.data.util.Log
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * SKFlipper AuctionHouseService
 * Version: 1.0.0
 * Compatible with: Minecraft 1.21.5 (Fabric), Kotlin 2.1.20
 *
 * Fetches and caches Hypixel SkyBlock Auction data.
 * - Supports pagination (multiple pages)
 * - Stores last snapshot + local cache
 * - Prepares for BIN / Flip analysis
 */
object AuctionHouseService {

    private const val CACHE_FILE = "auction_house_snapshot.json"
    private const val ENDPOINT = "https://api.hypixel.net/skyblock/auctions"
    private const val REFRESH_INTERVAL_MINUTES = 5L

    private val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "SKFlipper-AH-Refresh").apply { isDaemon = true }
    }

    private var running = false
    private var lastSnapshot: Snapshot? = null

    data class AuctionItem(
        val uuid: String,
        val itemName: String,
        val tier: String,
        val bin: Boolean,
        val startingBid: Double,
        val highestBid: Double,
        val end: Instant,
        val category: String,
        val seller: String?
    )

    data class Snapshot(
        val fetchedAt: Instant,
        val auctions: List<AuctionItem>,
        val totalPages: Int,
        val totalAuctions: Int
    )

    fun startAutoRefresh() {
        if (running) return
        running = true
        Log.info("Starting AuctionHouse auto-refresh loop ($REFRESH_INTERVAL_MINUTES min)")
        scheduler.scheduleAtFixedRate(
            { refreshNow() },
            0L, REFRESH_INTERVAL_MINUTES, TimeUnit.MINUTES
        )
    }

    fun currentSnapshot(): Snapshot? = lastSnapshot

    fun refreshNow(): CompletableFuture<Snapshot> {
        return CompletableFuture.supplyAsync {
            try {
                val first = HypixelApiClient.getJson(ENDPOINT)
                if (first == null || !first.get("success").asBoolean) {
                    Log.warn("AH fetch failed → using cached data if available")
                    val cache = CacheManager.readJsonObject(CACHE_FILE)
                    if (cache != null) {
                        lastSnapshot = parseCache(cache)
                        return@supplyAsync lastSnapshot!!
                    }
                    return@supplyAsync Snapshot(Instant.now(), emptyList(), 0, 0)
                }

                val totalPages = first["totalPages"]?.asInt ?: 0
                val auctions = mutableListOf<AuctionItem>()

                parsePage(first, auctions)

                // Fetch remaining pages (rate-limited to avoid spam)
                for (page in 1 until totalPages) {
                    Thread.sleep(250) // 4 req/sec max
                    val pageJson = HypixelApiClient.getJson("$ENDPOINT?page=$page")
                    if (pageJson != null && pageJson.get("success").asBoolean) {
                        parsePage(pageJson, auctions)
                    }
                }

                val snapshot = Snapshot(
                    fetchedAt = Instant.now(),
                    auctions = auctions,
                    totalPages = totalPages,
                    totalAuctions = auctions.size
                )

                CacheManager.writeJson(CACHE_FILE, snapshotToJson(snapshot))
                lastSnapshot = snapshot

                Log.info("Fetched AH snapshot: ${auctions.size} auctions ($totalPages pages)")
                snapshot
            } catch (e: Exception) {
                Log.error("AH refresh error: ${e.message}")
                lastSnapshot ?: Snapshot(Instant.now(), emptyList(), 0, 0)
            }
        }
    }

    private fun parsePage(json: JsonObject, auctions: MutableList<AuctionItem>) {
        try {
            val arr = json["auctions"]?.asJsonArray ?: return
            for (el in arr) {
                val obj = el.asJsonObject
                val itemName = obj["item_name"]?.asString ?: continue
                auctions += AuctionItem(
                    uuid = obj["uuid"]?.asString ?: "unknown",
                    itemName = itemName,
                    tier = obj["tier"]?.asString ?: "COMMON",
                    bin = obj["bin"]?.asBoolean ?: false,
                    startingBid = obj["starting_bid"]?.asDouble ?: 0.0,
                    highestBid = obj["highest_bid_amount"]?.asDouble ?: 0.0,
                    end = Instant.ofEpochMilli(obj["end"]?.asLong ?: 0L),
                    category = obj["category"]?.asString ?: "misc",
                    seller = obj["auctioneer"]?.asString
                )
            }
        } catch (e: Exception) {
            Log.error("AH page parse failed: ${e.message}")
        }
    }

    private fun snapshotToJson(snapshot: Snapshot): JsonObject {
        val json = JsonObject()
        json.addProperty("fetchedAt", snapshot.fetchedAt.toString())
        json.addProperty("totalPages", snapshot.totalPages)
        json.addProperty("totalAuctions", snapshot.totalAuctions)
        val arr = JsonArray()
        snapshot.auctions.forEach { a ->
            val obj = JsonObject()
            obj.addProperty("uuid", a.uuid)
            obj.addProperty("itemName", a.itemName)
            obj.addProperty("tier", a.tier)
            obj.addProperty("bin", a.bin)
            obj.addProperty("startingBid", a.startingBid)
            obj.addProperty("highestBid", a.highestBid)
            obj.addProperty("end", a.end.toString())
            obj.addProperty("category", a.category)
            obj.addProperty("seller", a.seller)
            arr.add(obj)
        }
        json.add("auctions", arr)
        return json
    }

    private fun parseCache(json: JsonObject): Snapshot {
        val fetchedAt = Instant.parse(json["fetchedAt"]?.asString ?: Instant.now().toString())
        val auctions = mutableListOf<AuctionItem>()
        val arr = json["auctions"]?.asJsonArray ?: JsonArray()
        for (el in arr) {
            val o = el.asJsonObject
            auctions += AuctionItem(
                uuid = o["uuid"]?.asString ?: "unknown",
                itemName = o["itemName"]?.asString ?: "Unknown",
                tier = o["tier"]?.asString ?: "COMMON",
                bin = o["bin"]?.asBoolean ?: false,
                startingBid = o["startingBid"]?.asDouble ?: 0.0,
                highestBid = o["highestBid"]?.asDouble ?: 0.0,
                end = try { Instant.parse(o["end"]?.asString) } catch (_: Exception) { Instant.now() },
                category = o["category"]?.asString ?: "misc",
                seller = o["seller"]?.asString
            )
        }
        Log.info("Loaded cached AH snapshot (${auctions.size} entries)")
        return Snapshot(fetchedAt, auctions, json["totalPages"]?.asInt ?: 0, auctions.size)
    }
}
