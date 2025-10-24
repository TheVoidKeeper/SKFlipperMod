package com.thevoidkeeper.skflipper.ui

import io.wispforest.owo.ui.base.BaseOwoScreen
import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.*
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.text.Text
import kotlin.math.pow

class SKFlipperScreen(private val screenTitle: Text) : BaseOwoScreen<FlowLayout>() {

    private lateinit var contentRoot: FlowLayout
    private lateinit var filterBar: FlowLayout
    private var activeTab: Tab = Tab.BAZAAR
    private val tabAnimations = mutableMapOf<Tab, Float>()

    override fun createAdapter(): OwoUIAdapter<FlowLayout> =
        OwoUIAdapter.create(this) { _, _ ->
            Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100))
        }

    override fun build(root: FlowLayout) {
        // Hintergrund abdunkeln
        root.surface { ctx, comp ->
            ctx.fill(
                comp.x(),
                comp.y(),
                comp.x() + comp.width(),
                comp.y() + comp.height(),
                0x88000000.toInt()
            )
        }

        root.gap(0)
        root.padding(Insets.of(10))
        root.horizontalAlignment(HorizontalAlignment.CENTER)

        val tabWidth = 110
        val tabHeight = 20
        val borderColor = 0xFFFFFFFF.toInt()

        // obere Linie
        root.child(Components.box(Sizing.fill(100), Sizing.fixed(1)).apply {
            color(Color.ofArgb(borderColor))
        })

        // Tabbar
        val tabBar = Containers.horizontalFlow(Sizing.content(), Sizing.fixed(tabHeight)).apply {
            horizontalAlignment(HorizontalAlignment.CENTER)
            gap(0)
        }

        fun drawCenteredText(ctx: OwoUIDrawContext, tr: TextRenderer, text: Text, cx: Int, cy: Int, color: Int) {
            val w = tr.getWidth(text)
            ctx.drawText(tr, text, cx - w / 2, cy - tr.fontHeight / 2, color, false)
        }

        fun tabButton(tab: Tab): ButtonComponent {
            val btn = Components.button(Text.literal(tab.label)) { switchTo(tab) }
            btn.sizing(Sizing.fixed(tabWidth), Sizing.fixed(tabHeight))
            tabAnimations[tab] = if (tab == activeTab) 1f else 0f

            btn.renderer { ctx: OwoUIDrawContext, self: ButtonComponent, _: Float ->
                val x = self.x()
                val y = self.y()
                val w = self.width()
                val h = self.height()
                val hovered = self.isHovered()
                val tr = MinecraftClient.getInstance().textRenderer

                val target = when {
                    tab == activeTab -> 1f
                    hovered -> 0.5f
                    else -> 0f
                }
                val speed = 0.15f
                val current = tabAnimations[tab] ?: 0f
                val state = current + (target - current) * speed
                tabAnimations[tab] = state

                val inactiveBg = 0x22000000
                val hoverBg = 0x33FFFFFF
                val activeBg = 0x55FFFFFF
                val bg = if (state < 0.5f)
                    interpolateColor(inactiveBg, hoverBg, state * 2f)
                else
                    interpolateColor(hoverBg, activeBg, (state - 0.5f) * 2f)
                ctx.fill(x, y, x + w, y + h, bg)

                if (tab == activeTab) {
                    val gold = 0xFFCC33
                    val thickness = 2
                    val tailLen = 48
                    val falloff = 2.1f
                    val waves = 2
                    val speedPxPerSec = 140.0
                    val perimeter = (w + h) * 2
                    val tSec = System.nanoTime() / 1_000_000_000.0
                    val head0 = (tSec * speedPxPerSec) % perimeter
                    val phaseShift = perimeter / waves
                    val heads = doubleArrayOf(head0, (head0 + phaseShift) % perimeter)

                    fun drawBorderPoint(pos: Int, alpha: Int) {
                        val color = ((alpha.coerceIn(0, 255) shl 24) or gold)
                        val p = ((pos % perimeter.toInt()) + perimeter.toInt()) % perimeter.toInt()
                        when {
                            p < w -> for (o in 0 until thickness) ctx.fill(x + p, y + o, x + p + 1, y + o + 1, color)
                            p < w + h -> for (o in 0 until thickness) ctx.fill(x + w - 1 - o, y + (p - w), x + w - o, y + (p - w) + 1, color)
                            p < w + h + w -> {
                                val off = p - (w + h)
                                for (o in 0 until thickness) ctx.fill(x + (w - 1 - off), y + h - 1 - o, x + (w - off), y + h - o, color)
                            }
                            else -> {
                                val off = p - (w + h + w)
                                for (o in 0 until thickness) ctx.fill(x + o, y + (h - 1 - off), x + o + 1, y + (h - off), color)
                            }
                        }
                    }
                    for (head in heads) {
                        val headInt = head.toInt()
                        for (s in 0 until tailLen) {
                            val pos = headInt - s
                            val p = s.toFloat() / tailLen.toFloat()
                            val alpha = (255.0 * (1.0 - p).pow(falloff.toDouble())).toInt()
                            drawBorderPoint(pos, alpha)
                        }
                    }
                }
                drawCenteredText(ctx, tr, Text.literal(tab.label), x + w / 2, y + h / 2, 0xFFFFFF)
            }
            return btn
        }

        tabBar.child(tabButton(Tab.AUCTION_HOUSE))
        tabBar.child(tabButton(Tab.BAZAAR))
        tabBar.child(tabButton(Tab.SETTINGS))
        root.child(tabBar)

        // untere Linie
        root.child(Components.box(Sizing.fill(100), Sizing.fixed(1)).apply {
            color(Color.ofArgb(borderColor))
        })

        // Filterleiste (einmalig)
        filterBar = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(26)).apply {
            horizontalAlignment(HorizontalAlignment.CENTER)
            gap(6)
            margins(Insets.of(5))

            fun label(s: String) = Components.label(Text.literal("$s:"))
            fun box(w: Int) = Components.textBox(Sizing.fixed(w))

            child(label("Search")); child(box(100))
            child(label("Budget")); child(box(80))
            child(label("Bulk")); child(box(80))
            child(label("Min Margin")); child(box(90))
            child(label("Min Volume")); child(box(90))
        }
        root.child(filterBar)

        // Content
        contentRoot = Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100)).apply {
            gap(6)
            margins(Insets.top(6))
        }
        root.child(contentRoot)

        updateFilterVisibility()
        rebuildContent(activeTab)
    }

    private fun interpolateColor(from: Int, to: Int, p: Float): Int {
        val fA = (from ushr 24) and 0xFF
        val fR = (from ushr 16) and 0xFF
        val fG = (from ushr 8) and 0xFF
        val fB = from and 0xFF
        val tA = (to ushr 24) and 0xFF
        val tR = (to ushr 16) and 0xFF
        val tG = (to ushr 8) and 0xFF
        val tB = to and 0xFF
        val pr = p.coerceIn(0f, 1f)
        val a = (fA + ((tA - fA) * pr)).toInt()
        val r = (fR + ((tR - fR) * pr)).toInt()
        val g = (fG + ((tG - fG) * pr)).toInt()
        val b = (fB + ((tB - fB) * pr)).toInt()
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun switchTo(tab: Tab) {
        if (tab == activeTab) return
        activeTab = tab
        updateFilterVisibility()
        rebuildContent(tab)
    }

    private fun updateFilterVisibility() {
        // Statt visible() nutzen wir einfach height ändern:
        if (activeTab == Tab.BAZAAR) {
            filterBar.sizing(Sizing.fill(100), Sizing.fixed(26))
            filterBar.padding(Insets.of(5))
            filterBar.gap(6)
        } else {
            filterBar.sizing(Sizing.fill(100), Sizing.fixed(0))
            filterBar.padding(Insets.of(0))
            filterBar.gap(0)
        }
    }

    private fun rebuildContent(tab: Tab) {
        contentRoot.clearChildren()
        when (tab) {
            Tab.BAZAAR -> {}
            Tab.AUCTION_HOUSE -> contentRoot.child(Components.label(Text.literal("→ AH support comes after Bazaar MVP.")))
            Tab.SETTINGS -> contentRoot.child(Components.label(Text.literal("→ YACL-based config screen will be opened from here.")))
        }
    }

    override fun shouldPause() = false

    enum class Tab(val label: String) {
        AUCTION_HOUSE("AuctionHouse"),
        BAZAAR("Bazaar"),
        SETTINGS("Settings")
    }
}
