package com.thevoidkeeper.skflipper.ui.components

import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.*

/** Platzhalter-Filterleiste für Auction House – gleiche API wie BazaarFilterBar */
class AuctionFilterBar(
    private val targetHeightPx: Int = 64,
    private val lerpFactor: Float = 0.25f
) {
    val root: FlowLayout = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(0)).apply {
        surface(Surface.VANILLA_TRANSLUCENT)
        padding(Insets.of(6))
        gap(8)
        alignment(HorizontalAlignment.LEFT, VerticalAlignment.CENTER)
        // später echte AH-Filter-Controls einfügen
    }

    private var active = false
    private var expanded = false
    private var progress = 0f

    fun setActive(value: Boolean) {
        active = value
        if (!active) {
            reset()
        } else {
            root.verticalSizing(Sizing.fixed(0))
            expanded = false
            progress = 0f
        }
    }

    fun toggle() {
        if (!active) return
        expanded = !expanded
    }

    fun reset() {
        expanded = false
        progress = 0f
        root.verticalSizing(Sizing.fixed(0))
    }

    fun tick(delta: Float) {
        if (!active) return
        val target = if (expanded) 1f else 0f
        progress += (target - progress) * (lerpFactor * delta.coerceIn(0f, 1f))
        val newHeight = (targetHeightPx * progress).toInt().coerceAtLeast(0)
        root.verticalSizing(Sizing.fixed(newHeight))
    }
}
