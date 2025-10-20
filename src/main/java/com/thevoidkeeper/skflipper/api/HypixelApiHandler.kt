package com.thevoidkeeper.skflipper.api

import com.google.gson.Gson
import com.thevoidkeeper.skflipper.config.SettingsManager
import com.thevoidkeeper.skflipper.util.CoroutineBus
import com.thevoidkeeper.skflipper.util.RateLimiter
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

object HypixelApiHandler {

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private fun limiter(): RateLimiter {
        val s = SettingsManager.settings
        return RateLimiter(s.maxConcurrentRequests, s.requestCooldownMs.milliseconds)
    }

    fun fetchBazaarAsync(): Deferred<BazaarResponse?> = CoroutineBus.io.async {
        limiter().run {
            val s = SettingsManager.settings
            val req = Request.Builder()
                .url("https://api.hypixel.net/v2/skyblock/bazaar")
                .apply { s.apiKey?.let { header("API-Key", it) } }
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                resp.body?.charStream()?.use {
                    return@use gson.fromJson(it, BazaarResponse::class.java)
                }
            }
            null
        }
    }

    fun fetchAuctionsAsync(page: Int = 0): Deferred<AuctionsResponse?> = CoroutineBus.io.async {
        limiter().run {
            val s = SettingsManager.settings
            val req = Request.Builder()
                .url("https://api.hypixel.net/v2/skyblock/auctions?page=$page")
                .apply { s.apiKey?.let { header("API-Key", it) } }
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                resp.body?.charStream()?.use {
                    return@use gson.fromJson(it, AuctionsResponse::class.java)
                }
            }
            null
        }
    }
}
