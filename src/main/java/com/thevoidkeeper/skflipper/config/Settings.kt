package com.thevoidkeeper.skflipper.config

data class Settings(
    var apiKey: String? = null,
    var maxConcurrentRequests: Int = 4,
    var requestCooldownMs: Long = 250L,

    var minProfit: Long = 10_000,
    var minMargin: Double = 0.05,
    var budgetCoins: Long = 5_000_000,
    var includeBulkItems: Boolean = true,
    var includeInstantBuySell: Boolean = true,

    var showRarityColors: Boolean = true,
    var showTooltips: Boolean = true
)
