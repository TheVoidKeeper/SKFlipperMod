package com.thevoidkeeper.skflipper.ui.components

import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.*
import net.minecraft.text.Text
import kotlin.math.max
import kotlin.math.min

/**
 * Ein-/ausklappbare Filterleiste für den Auction-House-Tab (OwoLib 0.12.21)
 * bleibt vollständig im dunklen Bereich und ordnet Filterfelder automatisch an.
 */
class AuctionFilterBar(
    private val targetHeightPx: Int = 84,
    private val lerpFactor: Float = 0.25f
) {

    val root: FlowLayout = Containers.verticalFlow(Sizing.fill(100), Sizing.fixed(0)).apply {
        surface(Surface.VANILLA_TRANSLUCENT)
        padding(Insets.of(8))
        gap(6)
        horizontalAlignment(HorizontalAlignment.CENTER)
        verticalAlignment(VerticalAlignment.TOP)

        // ─ Zeile 1 ─
        val row1 = Containers.horizontalFlow(Sizing.content(), Sizing.fixed(22)).apply {
            horizontalAlignment(HorizontalAlignment.CENTER)
            gap(10)
            child(labeledBox("Search:", 110))
            child(labeledBox("Rarity:", 80))
            child(labeledBox("BIN Only:", 60))
        }

        // ─ Zeile 2 ─
        val row2 = Containers.horizontalFlow(Sizing.content(), Sizing.fixed(22)).apply {
            horizontalAlignment(HorizontalAlignment.CENTER)
            gap(10)
            child(labeledBox("Duration:", 80))
            child(labeledBox("Budget:", 70))
            child(labeledBox("Seller:", 90))
            child(labeledBox("Item Type:", 90))
        }

        this.child(row1)
        this.child(row2)
    }

    private var active = false
    private var expanded = false
    private var progress = 0f

    private fun labeledBox(label: String, width: Int): FlowLayout {
        val labelText = Components.label(Text.literal(label))
        val box = Components.textBox(Sizing.fixed(width))
        return Containers.horizontalFlow(Sizing.content(), Sizing.fixed(20)).apply {
            gap(4)
            child(labelText)
            child(box)
        }
    }

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
        progress = min(1f, max(0f, progress))
        val newHeight = (targetHeightPx * progress).toInt().coerceAtLeast(0)
        root.verticalSizing(Sizing.fixed(newHeight))
    }
}
