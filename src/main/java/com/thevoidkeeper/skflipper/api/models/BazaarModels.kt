package com.thevoidkeeper.skflipper.api

data class BazaarResponse(
    val success: Boolean,
    val products: Map<String, BazaarProduct>?
)

data class BazaarProduct(
    val quick_status: QuickStatus?
)

data class QuickStatus(
    val productId: String,
    val sellPrice: Double,
    val sellVolume: Long,
    val sellMovingWeek: Long,
    val buyPrice: Double,
    val buyVolume: Long,
    val buyMovingWeek: Long
)
