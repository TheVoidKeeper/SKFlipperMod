package com.thevoidkeeper.skflipper;

import com.thevoidkeeper.skflipper.data.core.DataIntegrity;
import com.thevoidkeeper.skflipper.data.util.Log;
import com.thevoidkeeper.skflipper.ui.SKFlipperScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Hauptklasse des SKFlipper-Mods (Client-Side)
 * Initialisiert Datenebene, Logging, Keybinds und GUI.
 * Kompatibel mit Fabric 1.21.5 und OwoLib 0.12.21.
 */
public class SKFlipperMod implements ClientModInitializer {

    public static final String MOD_ID = "thevoidkeeper";
    private static KeyBinding openGuiKey;

    @Override
    public void onInitializeClient() {
        Log.INSTANCE.info("Initializing SKFlipperMod client...");

        // ── Datenintegritäts-Check vor Aktivierung ───────────────────────
        boolean valid = DataIntegrity.INSTANCE.validateALL();
        if (!valid) {
            Log.INSTANCE.error("Data layer validation failed – SKFlipper will run in safe mode.");
        } else {
            Log.INSTANCE.info("Data layer ready – Bazaar & AH refresh running.");
        }

        // ── Keybinding registrieren ───────────────────────────────────────
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skflipper.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.skflipper"
        ));

        // ── GUI öffnen bei Tastendruck (sicher pro Tick) ──────────────────
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                if (client.currentScreen == null) {
                    openScreen(client);
                }
            }
        });

        Log.INSTANCE.info("SKFlipperMod client initialization complete.");
    }

    /**
     * Öffnet das Hauptmenü des Mods (SKFlipperScreen)
     */
    private void openScreen(MinecraftClient client) {
        try {
            client.setScreen(new SKFlipperScreen(Text.translatable("title.skflipper")));
        } catch (Exception e) {
            Log.INSTANCE.error("Failed to open SKFlipperScreen: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
