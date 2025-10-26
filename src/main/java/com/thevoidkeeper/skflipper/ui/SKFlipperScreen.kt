package com.thevoidkeeper.skflipper.ui

import com.thevoidkeeper.skflipper.ui.components.AuctionFilterBar
import com.thevoidkeeper.skflipper.ui.components.BazaarFilterBar
import io.wispforest.owo.ui.base.BaseOwoScreen
import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.*
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import kotlin.math.pow

class SKFlipperScreen(private val screenTitle: Text) : BaseOwoScreen<FlowLayout>() {

    private lateinit var contentRoot: FlowLayout
    private lateinit var tabBar: FlowLayout
    private lateinit var filterToggle: ButtonComponent

    private lateinit var bazaarFilterBar: BazaarFilterBar
    private lateinit var auctionFilterBar: AuctionFilterBar

    private var activeTab: Tab = Tab.BAZAAR

    enum class Tab(val label: String) {
        AUCTION_HOUSE("AuctionHouse"),
        BAZAAR("Bazaar"),
        SETTINGS("Settings")
    }

    override fun createAdapter(): OwoUIAdapter<FlowLayout> {
        return OwoUIAdapter.create(this) { _, _ ->
            Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100))
        }
    }

    override fun build(root: FlowLayout) {
        // Hintergrund
        root.surface { ctx, comp ->
            ctx.fill(comp.x(), comp.y(), comp.x() + comp.width(), comp.y() + comp.height(), 0x88000000.toInt())
        }
        root.gap(0)
        root.padding(Insets.of(10))
        root.horizontalAlignment(HorizontalAlignment.CENTER)

        val borderColor = 0xFFFFFFFF.toInt()

        // ── obere Linie
        root.child(Components.box(Sizing.fill(100), Sizing.fixed(1)).apply {
            color(Color.ofArgb(borderColor))
        })

        val windowW = MinecraftClient.getInstance().window.scaledWidth
        val tabBarW = (windowW * 0.6f).toInt().coerceAtLeast(270)
        val tabCount = Tab.values().size
        val tabW = tabBarW / tabCount
        val tabH = 20

        // ── Tabs (mittig)
        tabBar = Containers.horizontalFlow(Sizing.fixed(tabBarW), Sizing.fixed(tabH)).apply {
            horizontalAlignment(HorizontalAlignment.CENTER)
            gap(0)
        }
        tabBar.child(makeTab(Tab.AUCTION_HOUSE, tabW, tabH))
        tabBar.child(makeTab(Tab.BAZAAR, tabW, tabH))
        tabBar.child(makeTab(Tab.SETTINGS, tabW, tabH))

        // ── Filter-Button (rechts neben Settings, gleiche Optik, nur 1× Text)
        filterToggle = Components.button(Text.empty()) {   // <— kein Doppeltext
            when (activeTab) {
                Tab.BAZAAR -> bazaarFilterBar.toggle()
                Tab.AUCTION_HOUSE -> auctionFilterBar.toggle()
                else -> {}
            }
        }.apply {
            // etwas schmaler als ein Tab (hier 70 % der Tab-Breite)
            sizing(Sizing.fixed((tabW * 0.7f).toInt()), Sizing.fixed(tabH))
            margins(Insets.left(16))
            renderer { ctx: DrawContext, self: ButtonComponent, _ ->
                val x = self.x()
                val y = self.y()
                val w = self.width()
                val h = self.height()
                val hovered = self.isHovered

                val bg = if (hovered) 0x55333333 else 0x33000000
                ctx.fill(x, y, x + w, y + h, bg)

                val white = 0xFFFFFFFF.toInt()
                // weiße Außenumrandung
                ctx.fill(x, y, x + w, y + 1, white)
                ctx.fill(x, y + h - 1, x + w, y + h, white)
                ctx.fill(x, y, x + 1, y + h, white)
                ctx.fill(x + w - 1, y, x + w, y + h, white)

                val tr = MinecraftClient.getInstance().textRenderer
                val label = Text.literal("Filters")
                val tx = x + (w - tr.getWidth(label)) / 2
                val ty = y + (h - tr.fontHeight) / 2
                ctx.drawText(tr, label, tx, ty, white, false)
            }
        }

        // ── Zeile: Tabs + Filter-Button
        val topRow = Containers.horizontalFlow(Sizing.content(), Sizing.fixed(tabH)).apply {
            horizontalAlignment(HorizontalAlignment.CENTER)
            gap(0)
        }
        topRow.child(tabBar)
        topRow.child(filterToggle)
        root.child(topRow)

        // ── weiße Linie DIREKT unter der Tab-/Filter-Zeile
        root.child(Components.box(Sizing.fill(100), Sizing.fixed(1)).apply {
            color(Color.ofArgb(borderColor))
        })

        // ── Filterleisten (standardmäßig eingeklappt)
        bazaarFilterBar = BazaarFilterBar()
        auctionFilterBar = AuctionFilterBar()
        root.child(bazaarFilterBar.root)
        root.child(auctionFilterBar.root)
        bazaarFilterBar.setActive(false)          // <— eingeklappt
        auctionFilterBar.setActive(false)         // <— eingeklappt

        // ── Content (hier später Grid/Stats)
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
        contentRoot.clearChildren()
        // passende Filterleiste aktivieren/deaktivieren
        bazaarFilterBar.setActive(tab == Tab.BAZAAR)
        auctionFilterBar.setActive(tab == Tab.AUCTION_HOUSE)
        // hier später Bazaar/AH-Content einhängen
    }

    // ✔ richtige Signatur für deine owo-lib 0.12.21
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        if (activeTab == Tab.BAZAAR) bazaarFilterBar.tick(delta)
        if (activeTab == Tab.AUCTION_HOUSE) auctionFilterBar.tick(delta)
    }

    // ───────────── Tab Rendering ─────────────

    private fun renderTab(ctx: DrawContext, self: ButtonComponent, tab: Tab) {
        val x = self.x()
        val y = self.y()
        val w = self.width()
        val h = self.height()
        val hovered = self.isHovered

        val baseColor = when {
            tab == activeTab -> 0x88333333.toInt()
            hovered -> 0x55333333.toInt()
            else -> 0x33000000
        }
        ctx.fill(x, y, x + w, y + h, baseColor)

        val tr: TextRenderer = MinecraftClient.getInstance().textRenderer
        val label = Text.literal(tab.label)
        val tx = x + (w - tr.getWidth(label)) / 2
        val ty = y + (h - tr.fontHeight) / 2
        ctx.drawText(tr, label, tx, ty, 0xFFFFFFFF.toInt(), false)

        if (tab == activeTab) drawCometBorder(ctx, x, y, w, h)
    }

    private fun drawCometBorder(ctx: DrawContext, x: Int, y: Int, w: Int, h: Int) {
        val gold = 0xFFFFD94B.toInt()
        val thickness = 2
        val speedPx = 180.0
        val tailLen = 56
        val falloff = 2.2f
        val perimeter = (w + h) * 2
        val tSec = System.nanoTime() / 1_000_000_000.0
        val head0 = (tSec * speedPx) % perimeter
        val phase = perimeter / 2.0
        val heads = doubleArrayOf(head0, (head0 + phase) % perimeter)

        fun putPixel(px: Int, py: Int, a: Int) {
            if (a <= 0) return
            val color = ((a and 0xFF) shl 24) or (gold and 0xFFFFFF)
            ctx.fill(px, py, px + 1, py + 1, color)
        }

        fun borderPoint(p: Int): Pair<Int, Int> {
            val pp = ((p % perimeter) + perimeter) % perimeter
            return when {
                pp < w -> Pair(x + pp, y)
                pp < w + h -> Pair(x + w - 1, y + (pp - w))
                pp < w + h + w -> Pair(x + (w - 1 - (pp - (w + h))), y + h - 1)
                else -> Pair(x, y + (h - 1 - (pp - (w + h + w))))
            }
        }

        for (head in heads) {
            val headI = head.toInt()
            for (s in 0 until tailLen) {
                val pos = headI - s
                val alpha = (255.0 * (1.0 - (s.toFloat() / tailLen).pow(falloff))).toInt()
                val (px, py) = borderPoint(pos)
                if (py == y) for (o in 0 until thickness) putPixel(px, py + o, alpha)
                else if (px == x + w - 1) for (o in 0 until thickness) putPixel(px - o, py, alpha)
                else if (py == y + h - 1) for (o in 0 until thickness) putPixel(px, py - o, alpha)
                else for (o in 0 until thickness) putPixel(px + o, py, alpha)
            }
        }
    }
}
