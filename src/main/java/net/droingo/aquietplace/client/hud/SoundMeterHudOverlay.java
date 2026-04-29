package net.droingo.aquietplace.client.hud;

import net.droingo.aquietplace.client.noise.ClientNoiseData;
import net.droingo.aquietplace.network.SoundMeterScanPayload;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public final class SoundMeterHudOverlay {
    private static final int PANEL_WIDTH = 170;
    private static final int BAR_WIDTH = 120;
    private static final int BAR_HEIGHT = 7;

    private SoundMeterHudOverlay() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register(SoundMeterHudOverlay::render);
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null || client.options.hudHidden) {
            return;
        }

        ClientSoundMeterData.tick();

        if (!ClientSoundMeterData.shouldRender()) {
            return;
        }

        float fade = ClientSoundMeterData.getFadeProgress();
        int alpha = Math.round(180.0f * fade) << 24;
        int textAlpha = Math.round(255.0f * fade) << 24;

        int x = 10;
        int y = 40;

        int playerNoisePercent = Math.round(ClientNoiseData.getNoiseLevel() * 100.0f);
        int ambientPercent = ClientSoundMeterData.getAmbientStrengthPercent();
        int maskingPercent = ClientSoundMeterData.getMaskingPercent();

        boolean hiddenByAmbient = playerNoisePercent <= ambientPercent;

        context.fill(
                x - 4,
                y - 4,
                x + PANEL_WIDTH + 18,
                y + 104,
                alpha | 0x00101010
        );

        context.drawText(
                client.textRenderer,
                "Sound Meter",
                x,
                y,
                textAlpha | 0x66CCFF,
                true
        );

        int lineY = y + 14;

        drawBar(
                context,
                client,
                x,
                lineY,
                "Your Noise",
                playerNoisePercent,
                getNoiseColor(playerNoisePercent) & 0x00FFFFFF,
                textAlpha
        );

        drawBar(
                context,
                client,
                x,
                lineY + 20,
                "Ambient",
                ambientPercent,
                getAmbientColor(ambientPercent) & 0x00FFFFFF,
                textAlpha
        );

        String status = hiddenByAmbient ? "Hidden by Ambient" : "Too Loud";
        int statusColor = hiddenByAmbient ? 0x55FF55 : 0xFF5555;

        context.drawText(
                client.textRenderer,
                "Masking: " + maskingPercent + "%",
                x,
                lineY + 42,
                textAlpha | 0xDDDDDD,
                true
        );

        context.drawText(
                client.textRenderer,
                status,
                x,
                lineY + 54,
                textAlpha | statusColor,
                true
        );

        context.drawText(
                client.textRenderer,
                "Sources: " + getSourceText(ClientSoundMeterData.getSourceFlags()),
                x,
                lineY + 66,
                textAlpha | 0xAAAAAA,
                true
        );
    }

    private static void drawBar(
            DrawContext context,
            MinecraftClient client,
            int x,
            int y,
            String label,
            int percent,
            int color,
            int textAlpha
    ) {
        int clampedPercent = Math.max(0, Math.min(100, percent));

        context.drawText(
                client.textRenderer,
                label + ": " + clampedPercent + "%",
                x,
                y,
                textAlpha | 0xFFFFFF,
                true
        );

        int barY = y + 10;
        int filledWidth = Math.round(BAR_WIDTH * (clampedPercent / 100.0f));

        context.fill(x, barY, x + BAR_WIDTH, barY + BAR_HEIGHT, 0xAA222222);
        context.fill(x, barY, x + filledWidth, barY + BAR_HEIGHT, 0xFF000000 | color);
        context.drawBorder(x, barY, BAR_WIDTH, BAR_HEIGHT, 0xFFFFFFFF);
    }

    private static int getNoiseColor(int percent) {
        if (percent < 8) {
            return 0xFF9E9E9E;
        }

        if (percent < 25) {
            return 0xFF55FF55;
        }

        if (percent < 55) {
            return 0xFFFFFF55;
        }

        if (percent < 85) {
            return 0xFFFFAA00;
        }

        return 0xFFFF3333;
    }

    private static int getAmbientColor(int percent) {
        if (percent < 10) {
            return 0xFF777777;
        }

        if (percent < 30) {
            return 0xFF55FF55;
        }

        if (percent < 55) {
            return 0xFFFFFF55;
        }

        if (percent < 80) {
            return 0xFFFFAA00;
        }

        return 0xFFFF3333;
    }

    private static String getSourceText(int sourceFlags) {
        StringBuilder builder = new StringBuilder();

        if ((sourceFlags & SoundMeterScanPayload.SOURCE_WATER) != 0) {
            appendSource(builder, "Water");
        }

        if ((sourceFlags & SoundMeterScanPayload.SOURCE_LAVA) != 0) {
            appendSource(builder, "Lava");
        }

        if ((sourceFlags & SoundMeterScanPayload.SOURCE_WEATHER) != 0) {
            appendSource(builder, "Weather");
        }
        if ((sourceFlags & SoundMeterScanPayload.SOURCE_WATERFALL) != 0) {
            appendSource(builder, "Waterfall");
        }

        if (builder.isEmpty()) {
            return "None";
        }

        return builder.toString();
    }

    private static void appendSource(StringBuilder builder, String source) {
        if (!builder.isEmpty()) {
            builder.append(", ");
        }

        builder.append(source);
    }
}