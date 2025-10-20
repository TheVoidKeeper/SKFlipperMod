package com.thevoidkeeper.skflipper

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW
import com.thevoidkeeper.skflipper.gui.SKFlipperScreen
import com.thevoidkeeper.skflipper.config.SettingsManager
import com.thevoidkeeper.skflipper.util.CoroutineBus

object SKFlipperMod : ClientModInitializer {

    const val MOD_ID = "thevoidkeeper"
    const val MOD_NAME = "SKFlipper"

    private lateinit var openGuiKey: KeyBinding

    override fun onInitializeClient() {
        SettingsManager.load()
        CoroutineBus.init()

        openGuiKey = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.$MOD_ID.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                "category.$MOD_ID"
            )
        )

        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client ->
            if (openGuiKey.wasPressed()) {
                client.setScreen(SKFlipperScreen())
            }
        })
    }
}
