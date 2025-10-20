package com.thevoidkeeper.skflipper.logic

import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting

object TextFormatter {
    fun rarityColor(tier: String?): Formatting = when (tier?.uppercase()) {
        "COMMON" -> Formatting.WHITE
        "UNCOMMON" -> Formatting.GREEN
        "RARE" -> Formatting.BLUE
        "EPIC" -> Formatting.DARK_PURPLE
        "LEGENDARY" -> Formatting.GOLD
        "MYTHIC" -> Formatting.LIGHT_PURPLE
        "DIVINE" -> Formatting.AQUA
        "SPECIAL" -> Formatting.RED
        "VERY_SPECIAL" -> Formatting.DARK_RED
        else -> Formatting.GRAY
    }

    fun colored(text: String, formatting: Formatting): Text =
        Text.literal(text).setStyle(Style.EMPTY.withColor(formatting))
}
