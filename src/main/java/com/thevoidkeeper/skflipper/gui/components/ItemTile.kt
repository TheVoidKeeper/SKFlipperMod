package com.thevoidkeeper.skflipper.gui.components

import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.core.Insets
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.Surface
import net.minecraft.text.Text

object ItemTile {
    fun create(title: String, subtitle: String) = Containers.verticalFlow(
        Sizing.fill(100), Sizing.content()
    ).apply {
        surface(Surface.VANILLA_TRANSLUCENT)
        padding(Insets.of(6))
        gap(2)
        child(Components.label(Text.literal(title)))
        child(Components.label(Text.literal(subtitle)))
    }
}
