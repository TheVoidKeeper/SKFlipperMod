package com.thevoidkeeper.skflipper;

import io.wispforest.owo.ui.core.OwoUIAdapter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class SKFlipperMod implements ClientModInitializer {

    public static final String MOD_ID = "thevoidkeeper";

    private static KeyBinding openGuiKey;

    @Override
    public void onInitializeClient() {
        // Keybinding to open the mod GUI
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skflipper.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.skflipper"
        ));

        // Open screen on key press (client tick safe)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new com.thevoidkeeper.skflipper.ui.SKFlipperScreen(Text.translatable("title.skflipper")));
                }
            }
        });
    }
}