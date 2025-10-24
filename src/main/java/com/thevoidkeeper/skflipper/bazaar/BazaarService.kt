package com.thevoidkeeper.skflipper.bazaar

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import java.util.concurrent.CompletableFuture

/**
 * Minimal bazaar snapshot fetcher. Uses only the JDK HttpClient.
 * Implementation intentionally keeps endpoints abstract so we don't lock to legacy URLs.
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

    fun fetchSnapshotAsync(): CompletableFuture<Snapshot>
}

class HttpBazaarService(
    private val endpoint: URI
) : BazaarService {

    private val client = HttpClient.newHttpClient()

    override fun fetchSnapshotAsync(): CompletableFuture<BazaarService.Snapshot> {
        val req = HttpRequest.newBuilder(endpoint).GET().build()
        return client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
            .thenApply { resp ->
                // TODO: parse JSON payload from the configured endpoint
                // Keep parsing implementation isolated to avoid API drift.
                // Return an empty snapshot for now; wire real parsing once endpoint is confirmed.
                BazaarService.Snapshot(
                    fetchedAt = Instant.now(),
                    products = emptyMap()
                )
            }
    }
}