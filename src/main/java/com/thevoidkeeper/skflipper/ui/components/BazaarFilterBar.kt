package com.thevoidkeeper.skflipper.ui.components

import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.*

import net.minecraft.text.Text
import kotlin.math.max
import kotlin.math.min

/**
 * Ein-/ausklappbare Filterleiste für den Bazaar-Tab (OwoLib 0.12.21)
 * - kein visibility/visible, nur Höhe 0 ↔ Zielhöhe
 * - active-Schalter steuert, ob getickt wird
 */
class BazaarFilterBar(
    private val targetHeightPx: Int = 84,
    private val lerpFactor: Float = 0.25f
) {

    val root: FlowLayout = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(0)).apply {
        surface(Surface.VANILLA_TRANSLUCENT)
        padding(Insets.of(6))
        gap(8)
        alignment(HorizontalAlignment.LEFT, VerticalAlignment.CENTER)

        // Platzhalter-Controls (später durch echte Controls ersetzen/binden)
        child(Components.label(Text.literal("Search:")))
        child(Components.textBox(Sizing.fixed(110)))

        child(Components.label(Text.literal("Category:")))
        child(Components.textBox(Sizing.fixed(90)))

        child(Components.label(Text.literal("Profit %:")))
        child(Components.textBox(Sizing.fixed(60)))

        child(Components.label(Text.literal("Volume:")))
        child(Components.textBox(Sizing.fixed(70)))

        child(Components.label(Text.literal("Order:")))
        child(Components.textBox(Sizing.fixed(80)))

        child(Components.label(Text.literal("Budget:")))
        child(Components.textBox(Sizing.fixed(70)))

        child(Components.label(Text.literal("Bulk:")))
        child(Components.textBox(Sizing.fixed(70)))
    }

    private var active = false
    private var expanded = false
    private var progress = 0f

    /** Vom Screen aufrufen, wenn der Tab aktiv/inaktiv wird */
    fun setActive(value: Boolean) {
        active = value
        if (!active) {
            reset()
        } else {
            // aktiv, aber standardmäßig eingeklappt
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

    /** pro Frame/Tick aufrufen */
    fun tick(delta: Float) {
        if (!active) return
        val target = if (expanded) 1f else 0f
        progress += (target - progress) * (lerpFactor * delta.coerceIn(0f, 1f))
        progress = min(1f, max(0f, progress))
        val newHeight = (targetHeightPx * progress).toInt().coerceAtLeast(0)
        root.verticalSizing(Sizing.fixed(newHeight))
    }
}
