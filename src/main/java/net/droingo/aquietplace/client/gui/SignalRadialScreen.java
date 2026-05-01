package net.droingo.aquietplace.client.gui;

import net.droingo.aquietplace.AQuietPlace;
import net.droingo.aquietplace.client.signal.ClientSignalColorSettings;
import net.droingo.aquietplace.network.SendPlayerSignalPayload;
import net.droingo.aquietplace.signal.SignalType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class SignalRadialScreen extends Screen {
    private static final int ICON_SIZE = 32;
    private static final int HOVERED_ICON_SIZE = 40;
    private static final int RADIUS = 78;
    private static final int INNER_DEAD_ZONE = 20;
    private static final int OUTER_LIMIT = 128;

    private static final int COLOR_SWATCH_SIZE = 18;
    private static final int COLOR_SWATCH_GAP = 6;

    private static final int[] SKIN_COLORS = {
            0xF8D8B0,
            0xF1C27D,
            0xE0AC69,
            0xC68642,
            0x8D5524,
            0x5C3522,
            0xD9B08C,
            0xA8754F
    };

    private final SignalType[] signals = SignalType.values();

    public SignalRadialScreen() {
        super(Text.translatable("screen.aquietplace.signal_radial"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Prevents Minecraft's normal menu blur.
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        context.fill(0, 0, this.width, this.height, 0x66000000);

        SignalType hoveredSignal = getHoveredSignal(mouseX, mouseY);

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.translatable("screen.aquietplace.signal_radial.title").formatted(Formatting.WHITE),
                centerX,
                centerY - 8,
                0xFFFFFF
        );

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.translatable("screen.aquietplace.signal_radial.hint").formatted(Formatting.GRAY),
                centerX,
                centerY + 8,
                0xAAAAAA
        );

        for (int i = 0; i < signals.length; i++) {
            drawSignalIcon(context, signals[i], i, hoveredSignal, centerX, centerY);
        }

        drawColorSelector(context, mouseX, mouseY, centerX, centerY + 145);

        if (hoveredSignal != null) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.literal("Selected: ").formatted(Formatting.GRAY)
                            .append(hoveredSignal.getDisplayName().copy().formatted(Formatting.YELLOW)),
                    centerX,
                    centerY + 28,
                    0xFFFFFF
            );
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawSignalIcon(
            DrawContext context,
            SignalType signal,
            int index,
            SignalType hoveredSignal,
            int centerX,
            int centerY
    ) {
        double angle = getAngleForIndex(index);
        int iconSize = signal == hoveredSignal ? HOVERED_ICON_SIZE : ICON_SIZE;

        int iconCenterX = centerX + (int) Math.round(Math.cos(angle) * RADIUS);
        int iconCenterY = centerY + (int) Math.round(Math.sin(angle) * RADIUS);

        int iconX = iconCenterX - iconSize / 2;
        int iconY = iconCenterY - iconSize / 2;

        if (signal == hoveredSignal) {
            context.fill(
                    iconX - 4,
                    iconY - 4,
                    iconX + iconSize + 4,
                    iconY + iconSize + 4,
                    0xAAFFFFFF
            );
        } else {
            context.fill(
                    iconX - 3,
                    iconY - 3,
                    iconX + iconSize + 3,
                    iconY + iconSize + 3,
                    0x88000000
            );
        }

        context.drawTexture(
                getSignalIcon(signal),
                iconX,
                iconY,
                0.0f,
                0.0f,
                iconSize,
                iconSize,
                iconSize,
                iconSize
        );

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                signal.getDisplayName(),
                iconCenterX,
                iconY + iconSize + 5,
                signal == hoveredSignal ? 0xFFFF55 : 0xDDDDDD
        );
    }

    private void drawColorSelector(DrawContext context, int mouseX, int mouseY, int centerX, int y) {
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.translatable("screen.aquietplace.signal_radial.skin_color").formatted(Formatting.GRAY),
                centerX,
                y - 16,
                0xAAAAAA
        );

        int totalWidth = SKIN_COLORS.length * COLOR_SWATCH_SIZE
                + (SKIN_COLORS.length - 1) * COLOR_SWATCH_GAP;

        int startX = centerX - totalWidth / 2;
        int selectedColor = ClientSignalColorSettings.getSelectedColorRgb();

        for (int i = 0; i < SKIN_COLORS.length; i++) {
            int x = startX + i * (COLOR_SWATCH_SIZE + COLOR_SWATCH_GAP);
            int color = SKIN_COLORS[i];

            boolean hovered = isInside(mouseX, mouseY, x, y, COLOR_SWATCH_SIZE, COLOR_SWATCH_SIZE);
            boolean selected = selectedColor == color;

            int borderColor = selected ? 0xFFFFFFFF : hovered ? 0xFFFFFF55 : 0xAA000000;

            context.fill(
                    x - 2,
                    y - 2,
                    x + COLOR_SWATCH_SIZE + 2,
                    y + COLOR_SWATCH_SIZE + 2,
                    borderColor
            );

            context.fill(
                    x,
                    y,
                    x + COLOR_SWATCH_SIZE,
                    y + COLOR_SWATCH_SIZE,
                    0xFF000000 | color
            );
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Integer clickedColor = getClickedColor(mouseX, mouseY);

        if (clickedColor != null) {
            ClientSignalColorSettings.setSelectedColorRgb(clickedColor);
            return true;
        }

        SignalType hoveredSignal = getHoveredSignal(mouseX, mouseY);

        if (hoveredSignal == null) {
            return true;
        }

        ClientPlayNetworking.send(
                new SendPlayerSignalPayload(
                        hoveredSignal.toNetworkId(),
                        ClientSignalColorSettings.getSelectedColorRgb()
                )
        );

        this.close();

        return true;
    }

    private Integer getClickedColor(double mouseX, double mouseY) {
        int centerX = this.width / 2;
        int y = this.height / 2 + 145;

        int totalWidth = SKIN_COLORS.length * COLOR_SWATCH_SIZE
                + (SKIN_COLORS.length - 1) * COLOR_SWATCH_GAP;

        int startX = centerX - totalWidth / 2;

        for (int i = 0; i < SKIN_COLORS.length; i++) {
            int x = startX + i * (COLOR_SWATCH_SIZE + COLOR_SWATCH_GAP);

            if (isInside(mouseX, mouseY, x, y, COLOR_SWATCH_SIZE, COLOR_SWATCH_SIZE)) {
                return SKIN_COLORS[i];
            }
        }

        return null;
    }

    private SignalType getHoveredSignal(double mouseX, double mouseY) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance < INNER_DEAD_ZONE || distance > OUTER_LIMIT) {
            return null;
        }

        double angle = Math.atan2(dy, dx);
        double shiftedAngle = angle + Math.PI / 2.0;

        while (shiftedAngle < 0.0) {
            shiftedAngle += Math.PI * 2.0;
        }

        double sliceSize = Math.PI * 2.0 / signals.length;
        int index = (int) Math.floor((shiftedAngle + sliceSize / 2.0) / sliceSize) % signals.length;

        return signals[index];
    }

    private double getAngleForIndex(int index) {
        double sliceSize = Math.PI * 2.0 / signals.length;
        return -Math.PI / 2.0 + index * sliceSize;
    }

    private Identifier getSignalIcon(SignalType signalType) {
        return Identifier.of(
                AQuietPlace.MOD_ID,
                "textures/gui/signal/signal_" + signalType.getId() + "_0.png"
        );
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x
                && mouseX < x + width
                && mouseY >= y
                && mouseY < y + height;
    }
}