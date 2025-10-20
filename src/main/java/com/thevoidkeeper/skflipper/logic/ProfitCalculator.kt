package com.thevoidkeeper.skflipper.logic

object ProfitCalculator {
    private const val BAZAAR_TAX = 0.0125

    fun bazaarFlipProfit(buyPrice: Double, sellPrice: Double, amount: Int): Long {
        val gross = (sellPrice - buyPrice) * amount
        val tax = sellPrice * amount * BAZAAR_TAX
        return (gross - tax).toLong()
    }

    fun margin(buyPrice: Double, sellPrice: Double): Double {
        if (buyPrice <= 0.0) return 0.0
        return (sellPrice - buyPrice) / buyPrice
    }
}
