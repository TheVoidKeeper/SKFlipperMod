package com.thevoidkeeper.skflipper.bazaar

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.Volatile

/**
 * Abfrage- und Refresh-Logik für Hypixel Bazaar-Daten.
 * Versionskompatibel mit Fabric 1.21.5 + Kotlin 2.1.20.
 */
interface BazaarService {

    data class Snapshot(
        val fetchedAt: Instant,
        val products: Map<String, Product>
    )

    data class Product(
        val productId: String,
        val sellPrice: Double,
        val buyPrice: Double,
        val sellVolume: Long,
        val buyVolume: Long
    )

    fun currentSnapshot(): Snapshot?
    fun fetchSnapshotAsync(): CompletableFuture<Snapshot>
}

/**
 * HTTP-Implementierung des BazaarService mit Auto-Refresh-Loop.
 * Kein Modrinth-API-Key nötig.
 */
class HttpBazaarService(
    private val endpoint: URI = URI("https://api.hypixel.net/skyblock/bazaar"),
    private val refreshIntervalSeconds: Long = 10
) : BazaarService {

    private val client = HttpClient.newBuilder()
        .connectTimeout(java.time.Duration.ofSeconds(5))
        .build()

    @Volatile
    private var lastSnapshot: BazaarService.Snapshot? = null

    private val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "Bazaar-Refresh-Thread").apply { isDaemon = true }
    }

    init {
        // Startet Auto-Refresh-Loop
        scheduler.scheduleAtFixedRate(
            { safeRefresh() },
            0L,
            refreshIntervalSeconds,
            TimeUnit.SECONDS
        )
    }

    override fun currentSnapshot(): BazaarService.Snapshot? = lastSnapshot

    override fun fetchSnapshotAsync(): CompletableFuture<BazaarService.Snapshot> {
        val req = HttpRequest.newBuilder(endpoint).GET().build()
        return client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
            .thenApply { resp ->
                if (resp.statusCode() != 200) {
                    println("[SKFlipper] Bazaar HTTP Error ${resp.statusCode()}")
                    lastSnapshot ?: BazaarService.Snapshot(Instant.now(), emptyMap())
                } else {
                    parseSnapshot(resp.body())
                }
            }
            .exceptionally { ex ->
                println("[SKFlipper] Bazaar fetch failed: ${ex.message}")
                lastSnapshot ?: BazaarService.Snapshot(Instant.now(), emptyMap())
            }
    }

    private fun safeRefresh() {
        try {
            fetchSnapshotAsync().thenAccept { snapshot ->
                lastSnapshot = snapshot
            }
        } catch (e: Exception) {
            println("[SKFlipper] Bazaar refresh error: ${e.message}")
        }
    }

    private fun parseSnapshot(body: String): BazaarService.Snapshot {
        val json = JsonParser.parseString(body).asJsonObject
        val success = json["success"]?.asBoolean ?: false
        if (!success) return BazaarService.Snapshot(Instant.now(), emptyMap())

        val productsJson = json["products"].asJsonObject
        val products = mutableMapOf<String, BazaarService.Product>()

        for ((id, entryElem) in productsJson.entrySet()) {
            val productObj = entryElem.asJsonObject
            val quickStatus = productObj["quick_status"]?.asJsonObject ?: continue

            val sellPrice = quickStatus["sellPrice"]?.asDouble ?: 0.0
            val buyPrice = quickStatus["buyPrice"]?.asDouble ?: 0.0
            val sellVolume = quickStatus["sellVolume"]?.asLong ?: 0L
            val buyVolume = quickStatus["buyVolume"]?.asLong ?: 0L

            products[id] = BazaarService.Product(
                productId = id,
                sellPrice = sellPrice,
                buyPrice = buyPrice,
                sellVolume = sellVolume,
                buyVolume = buyVolume
            )
        }

        return BazaarService.Snapshot(
            fetchedAt = Instant.now(),
            products = products
        )
    }
}
