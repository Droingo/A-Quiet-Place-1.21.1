package net.droingo.aquietplace.client.keybind;

import net.droingo.aquietplace.client.gui.SignalRadialScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class ModKeybinds {
    private static KeyBinding signalMenuKey;

    private ModKeybinds() {
    }

    public static void register() {
        signalMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.aquietplace.signal_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "key.category.aquietplace"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (signalMenuKey.wasPressed()) {
                if (client.player == null || client.getNetworkHandler() == null) {
                    return;
                }

                client.setScreen(new SignalRadialScreen());
            }
        });
    }
}