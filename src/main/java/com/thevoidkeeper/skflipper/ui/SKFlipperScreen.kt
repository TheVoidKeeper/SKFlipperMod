package com.thevoidkeeper.skflipper.ui

import io.wispforest.owo.ui.base.BaseOwoScreen
import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.Color
import io.wispforest.owo.ui.core.HorizontalAlignment
import io.wispforest.owo.ui.core.Insets
import io.wispforest.owo.ui.core.OwoUIAdapter
import io.wispforest.owo.ui.core.Sizing
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import kotlin.math.pow

class SKFlipperScreen(private val screenTitle: Text) : BaseOwoScreen<FlowLayout>() {

    private lateinit var contentRoot: FlowLayout
    private var activeTab: Tab = Tab.BAZAAR

    enum class Tab(val label: String) {
        AUCTION_HOUSE("AuctionHouse"),
        BAZAAR("Bazaar"),
        SETTINGS("Settings")
    }

    override fun createAdapter(): OwoUIAdapter<FlowLayout> {
        // Exakte Signatur für owo-lib 0.12.21
        return OwoUIAdapter.create(this) { _, _ ->
            Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100))
        }
    }

    override fun build(root: FlowLayout) {
        // EINMALIGER, halbtransparenter Hintergrund
        root.surface { ctx, comp ->
            ctx.fill(comp.x(), comp.y(), comp.x() + comp.width(), comp.y() + comp.height(), 0x88000000.toInt())
        }

        root.gap(0)
        root.padding(Insets.of(10))
        root.horizontalAlignment(HorizontalAlignment.CENTER)

        val borderColor = 0xFFFFFFFF.toInt()

        // OBERE LINIE (vollbreit)
        root.child(Components.box(Sizing.fill(100), Sizing.fixed(1)).apply {
            color(Color.ofArgb(borderColor))
        })

        // TABS – mittig, gleich breit
        val windowW = MinecraftClient.getInstance().window.scaledWidth
        val tabBarW = (windowW * 0.6f).toInt().coerceAtLeast(270)
        val tabCount = Tab.values().size
        val tabW = tabBarW / tabCount
        val tabH = 20

        val tabBar = Containers.horizontalFlow(Sizing.fixed(tabBarW), Sizing.fixed(tabH)).apply {
            horizontalAlignment(HorizontalAlignment.CENTER)
            gap(0)
        }
        tabBar.child(makeTab(Tab.AUCTION_HOUSE, tabW, tabH))
        tabBar.child(makeTab(Tab.BAZAAR, tabW, tabH))
        tabBar.child(makeTab(Tab.SETTINGS, tabW, tabH))
        root.child(tabBar)

        // UNTERE LINIE (vollbreit, direkt unter Tabs)
        root.child(Components.box(Sizing.fill(100), Sizing.fixed(1)).apply {
            color(Color.ofArgb(borderColor))
        })

        // CONTENT
        contentRoot = Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100)).apply { gap(6) }
        root.child(contentRoot)

        rebuildContent(activeTab)
    }

    private fun makeTab(tab: Tab, w: Int, h: Int): ButtonComponent {
        val btn = Components.button(Text.empty()) { switchTo(tab) }
        btn.sizing(Sizing.fixed(w), Sizing.fixed(h))
        btn.margins(Insets.none())

        btn.renderer { ctx: DrawContext, self: ButtonComponent, _ ->
            renderTab(ctx, self, tab)
        }
        return btn
    }

    private fun switchTo(tab: Tab) {
        if (tab == activeTab) return
        activeTab = tab
        rebuildContent(tab)
    }

    private fun rebuildContent(tab: Tab) {
        contentRoot.clearChildren() // wichtig: kompatible API
        val text = when (tab) {
            Tab.AUCTION_HOUSE -> Text.literal("➜ AH support comes after Bazaar MVP.")
            Tab.BAZAAR        -> Text.literal("➜ Live Bazaar snapshot, margins & volume will appear here.")
            Tab.SETTINGS      -> Text.literal("➜ YACL-based config screen will be opened from here.")
        }
        contentRoot.child(Components.label(text))
    }

    // ----------------- Rendering -----------------

    private fun renderTab(ctx: DrawContext, self: ButtonComponent, tab: Tab) {
        val x = self.x()
        val y = self.y()
        val w = self.width()
        val h = self.height()
        val hovered = self.isHovered

        // Hintergründe wie vorher
        val baseColor = when {
            tab == activeTab -> 0x88333333.toInt()
            hovered         -> 0x55333333.toInt()
            else            -> 0x33000000
        }
        ctx.fill(x, y, x + w, y + h, baseColor)

        // Text mittig (kein Duplikat, weil Button-Message leer)
        val tr: TextRenderer = MinecraftClient.getInstance().textRenderer
        val label = Text.literal(tab.label)
        val tx = x + (w - tr.getWidth(label)) / 2
        val ty = y + (h - tr.fontHeight) / 2
        ctx.drawText(tr, label, tx, ty, 0xFFFFFFFF.toInt(), false)

        // Nur der aktive Tab bekommt die Sternschnuppen-Animation im Rahmen
        if (tab == activeTab) drawCometBorder(ctx, x, y, w, h)
    }

    /**
     * Zwei „Sternschnuppen“ laufen umlaufend im Rahmen (nur innerhalb),
     * mit langem, weichem Tail (gold → transparent).
     */
    private fun drawCometBorder(ctx: DrawContext, x: Int, y: Int, w: Int, h: Int) {
        val gold = 0xFFFFD94B.toInt()
        val thickness = 2                  // Rahmenstärke (bleibt innerhalb)
        val speedPx = 180.0                // Geschwindigkeit (px pro Sekunde)
        val tailLen = 56                   // Tail-Länge
        val falloff = 2.2f                 // Tail-Falloff
        val perimeter = (w + h) * 2
        val tSec = System.nanoTime() / 1_000_000_000.0
        val head0 = (tSec * speedPx) % perimeter
        val phase = perimeter / 2.0        // 2 Läufer gegenüber
        val heads = doubleArrayOf(head0, (head0 + phase) % perimeter)

        fun putPixel(px: Int, py: Int, a: Int) {
            if (a <= 0) return
            val color = ((a and 0xFF) shl 24) or (gold and 0xFFFFFF)
            ctx.fill(px, py, px + 1, py + 1, color)
        }

        fun borderPoint(p: Int): Pair<Int, Int> {
            val pp = ((p % perimeter) + perimeter) % perimeter
            return when {
                pp < w             -> Pair(x + pp,           y)            // top
                pp < w + h         -> Pair(x + w - 1,       y + (pp - w))  // right
                pp < w + h + w     -> Pair(x + (w - 1 - (pp - (w + h))), y + h - 1) // bottom
                else               -> Pair(x,                 y + (h - 1 - (pp - (w + h + w)))) // left
            }
        }

        // Rahmen“dicke“ innerhalb zeichnen
        for (head in heads) {
            val headI = head.toInt()
            for (s in 0 until tailLen) {
                val pos = headI - s
                val alpha = (255.0 * (1.0 - (s.toFloat() / tailLen).pow(falloff))).toInt()
                val (px, py) = borderPoint(pos)
                // top/bottom ziehen horizontal in die Fläche, right/left vertikal in die Fläche
                if      (py == y)           for (o in 0 until thickness) putPixel(px, py + o, alpha)          // top
                else if (px == x + w - 1)   for (o in 0 until thickness) putPixel(px - o, py, alpha)          // right
                else if (py == y + h - 1)   for (o in 0 until thickness) putPixel(px, py - o, alpha)          // bottom
                else /* px == x */          for (o in 0 until thickness) putPixel(px + o, py, alpha)          // left
            }
        }
    }
}
