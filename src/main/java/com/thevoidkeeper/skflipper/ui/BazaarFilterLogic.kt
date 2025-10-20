package com.thevoidkeeper.skflipper.ui.logic

data class BazaarItem(
    val name: String,
    val buyPrice: Double,
    val sellPrice: Double,
    val volume: Int,
    val marginPercent: Double,
    val bulkAmount: Int
)

object BazaarFilterLogic {

    fun filterItems(
        items: List<BazaarItem>,
        search: String?,
        budget: Double?,
        bulk: Int?,
        minMargin: Double?,
        minVolume: Int?,
        sortBy: String?
    ): List<BazaarItem> {

        var filtered = items

        if (!search.isNullOrBlank()) {
            filtered = filtered.filter { it.name.contains(search, ignoreCase = true) }
        }

        if (budget != null) {
            filtered = filtered.filter { it.buyPrice * (bulk ?: 1) <= budget }
        }

        if (bulk != null) {
            filtered = filtered.filter { it.bulkAmount >= bulk }
        }

        if (minMargin != null) {
            filtered = filtered.filter { it.marginPercent >= minMargin }
        }

        if (minVolume != null) {
            filtered = filtered.filter { it.volume >= minVolume }
        }

        filtered = when (sortBy?.lowercase()) {
            "profit" -> filtered.sortedByDescending { (it.sellPrice - it.buyPrice) * it.bulkAmount }
            "volume" -> filtered.sortedByDescending { it.volume }
            "buy price" -> filtered.sortedBy { it.buyPrice }
            "sell price" -> filtered.sortedByDescending { it.sellPrice }
            else -> filtered
        }

        return filtered
    }
}
