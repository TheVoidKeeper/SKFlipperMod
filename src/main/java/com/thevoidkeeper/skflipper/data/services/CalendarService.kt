package com.thevoidkeeper.skflipper.data.services


import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.thevoidkeeper.skflipper.data.core.HypixelApiClient
import com.thevoidkeeper.skflipper.data.core.CacheManager
import com.thevoidkeeper.skflipper.data.util.Log
import java.time.Instant

/**
 * SKFlipper CalendarService
 * Version: 1.0.0
 * Compatible with: Minecraft 1.21.5 (Fabric), Kotlin 2.1.20
 *
 * Fetches and caches SkyBlock calendar events for future trend correlation.
 * Cached under: /config/SKFlipper/cache/calendar_events.json
 */
object CalendarService {

    private const val CACHE_FILE = "calendar_events.json"
    private const val ENDPOINT = "https://api.hypixel.net/resources/skyblock/calendar"

    private var lastUpdated: Instant? = null
    private val events = mutableListOf<CalendarEvent>()

    data class CalendarEvent(
        val id: String,
        val name: String,
        val start: Instant,
        val end: Instant,
        val description: String?,
        val repeatable: Boolean = false
    )

    fun load(forceRefresh: Boolean = false): List<CalendarEvent> {
        if (events.isNotEmpty() && !forceRefresh) return events

        val cache = CacheManager.readJsonObject(CACHE_FILE)
        val cacheAgeOk = cache != null && cache["lastUpdated"]?.asString?.let {
            try { Instant.parse(it).isAfter(Instant.now().minusSeconds(86_400)) } catch (_: Exception) { false }
        } ?: false

        if (!forceRefresh && cacheAgeOk) {
            Log.info("Loaded Calendar cache from local file")
            loadFromCache(cache!!)
            return events
        }

        Log.info("Fetching SkyBlock calendar events from Hypixel API…")
        val json = HypixelApiClient.getJson(ENDPOINT, attachKey = false)
        if (json == null || !json.has("events")) {
            Log.warn("Failed to fetch calendar data, using cache if available")
            if (cache != null) loadFromCache(cache)
            return events
        }

        parseAndCache(json)
        return events
    }

    private fun loadFromCache(json: JsonObject) {
        try {
            val arr = json["events"]?.asJsonArray ?: return
            events.clear()
            arr.forEach { el ->
                val o = el.asJsonObject
                val id = o["id"]?.asString ?: return@forEach
                val start = try { Instant.parse(o["start"]?.asString) } catch (_: Exception) { Instant.now() }
                val end = try { Instant.parse(o["end"]?.asString) } catch (_: Exception) { Instant.now() }
                events += CalendarEvent(
                    id = id,
                    name = o["name"]?.asString ?: id,
                    start = start,
                    end = end,
                    description = o["description"]?.asString,
                    repeatable = o["repeatable"]?.asBoolean ?: false
                )
            }
            lastUpdated = Instant.parse(json["lastUpdated"]?.asString ?: Instant.now().toString())
            Log.info("Calendar cache loaded (${events.size} events)")
        } catch (e: Exception) {
            Log.error("Failed to parse cached calendar events: ${e.message}")
        }
    }

    private fun parseAndCache(json: JsonObject) {
        try {
            val arr = json["events"]?.asJsonArray ?: JsonArray()
            events.clear()
            for (el in arr) {
                val obj = el.asJsonObject
                val id = obj["id"]?.asString ?: continue
                val start = try { Instant.parse(obj["start"]?.asString) } catch (_: Exception) { Instant.now() }
                val end = try { Instant.parse(obj["end"]?.asString) } catch (_: Exception) { Instant.now() }
                events += CalendarEvent(
                    id = id,
                    name = obj["name"]?.asString ?: id,
                    start = start,
                    end = end,
                    description = obj["description"]?.asString,
                    repeatable = obj["repeatable"]?.asBoolean ?: false
                )
            }

            val output = JsonObject().apply {
                addProperty("lastUpdated", Instant.now().toString())
                val arrOut = JsonArray()
                events.forEach {
                    val o = JsonObject()
                    o.addProperty("id", it.id)
                    o.addProperty("name", it.name)
                    o.addProperty("start", it.start.toString())
                    o.addProperty("end", it.end.toString())
                    o.addProperty("description", it.description)
                    o.addProperty("repeatable", it.repeatable)
                    arrOut.add(o)
                }
                add("events", arrOut)
            }

            CacheManager.writeJson(CACHE_FILE, output)
            Log.info("Fetched and cached ${events.size} calendar events.")
        } catch (e: Exception) {
            Log.error("Error parsing calendar data: ${e.message}")
        }
    }

    fun getActiveEvents(now: Instant = Instant.now()): List<CalendarEvent> {
        return events.filter { now.isAfter(it.start) && now.isBefore(it.end) }
    }

    fun getUpcomingEvents(now: Instant = Instant.now()): List<CalendarEvent> {
        return events.filter { it.start.isAfter(now) }.sortedBy { it.start }
    }
}
