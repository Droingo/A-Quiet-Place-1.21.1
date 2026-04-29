package net.droingo.aquietplace.client.hud;

import net.droingo.aquietplace.client.noise.ClientNoiseData;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public final class NoiseHudOverlay {
    private static final int BAR_WIDTH = 82;
    private static final int BAR_HEIGHT = 8;

    private NoiseHudOverlay() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register(NoiseHudOverlay::render);
    }

    private static void render(DrawContext drawContext, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null || client.options.hudHidden) {
            return;
        }

        if (ClientSoundMeterData.shouldRender()) {
            return;
        }

        float noiseLevel = ClientNoiseData.getNoiseLevel();

        int x = 10;
        int y = 10;

        String label = getNoiseLabel(noiseLevel);
        int color = getNoiseColor(noiseLevel);

        drawContext.fill(x - 3, y - 3, x + BAR_WIDTH + 3, y + 24, 0x88000000);

        drawContext.drawText(
                client.textRenderer,
                "Noise: " + label,
                x,
                y,
                color,
                true
        );

        int barY = y + 13;
        int filledWidth = Math.round(BAR_WIDTH * noiseLevel);

        drawContext.fill(x, barY, x + BAR_WIDTH, barY + BAR_HEIGHT, 0xAA222222);
        drawContext.fill(x, barY, x + filledWidth, barY + BAR_HEIGHT, color | 0xFF000000);
        drawContext.drawBorder(x, barY, BAR_WIDTH, BAR_HEIGHT, 0xFFFFFFFF);
    }

    private static String getNoiseLabel(float noiseLevel) {
        if (noiseLevel < 0.08f) {
            return "Silent";
        }

        if (noiseLevel < 0.25f) {
            return "Quiet";
        }

        if (noiseLevel < 0.55f) {
            return "Audible";
        }

        if (noiseLevel < 0.85f) {
            return "Loud";
        }

        return "Dangerous";
    }

    private static int getNoiseColor(float noiseLevel) {
        if (noiseLevel < 0.08f) {
            return 0xFF9E9E9E;
        }

        if (noiseLevel < 0.25f) {
            return 0xFF55FF55;
        }

        if (noiseLevel < 0.55f) {
            return 0xFFFFFF55;
        }

        if (noiseLevel < 0.85f) {
            return 0xFFFFAA00;
        }

        return 0xFFFF3333;
    }
}