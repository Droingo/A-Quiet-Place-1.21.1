package net.droingo.aquietplace.client.screen;

import net.droingo.aquietplace.network.NoisemakerOpenScreenPayload;
import net.droingo.aquietplace.network.NoisemakerSaveSettingsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class NoisemakerScreen extends Screen {
    private final BlockPos pos;

    private int delaySeconds;
    private float radius;
    private float strength;
    private boolean armed;

    public NoisemakerScreen(NoisemakerOpenScreenPayload payload) {
        super(Text.literal("Noisemaker"));
        this.pos = payload.pos();
        this.delaySeconds = payload.delaySeconds();
        this.radius = payload.radius();
        this.strength = payload.strength();
        this.armed = payload.armed();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - 70;

        addSettingButtons(centerX, startY, "Delay", () -> this.delaySeconds + "s",
                () -> this.delaySeconds = Math.max(0, this.delaySeconds - 1),
                () -> this.delaySeconds = Math.min(60, this.delaySeconds + 1)
        );

        addSettingButtons(centerX, startY + 32, "Radius", () -> String.format("%.1f", this.radius),
                () -> this.radius = Math.max(1.0f, this.radius - 1.0f),
                () -> this.radius = Math.min(128.0f, this.radius + 1.0f)
        );

        addSettingButtons(centerX, startY + 64, "Strength", () -> String.format("%.2f", this.strength),
                () -> this.strength = Math.max(0.05f, this.strength - 0.05f),
                () -> this.strength = Math.min(1.0f, this.strength + 0.05f)
        );

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal(this.armed ? "Armed: ON" : "Armed: OFF"),
                button -> {
                    this.armed = !this.armed;
                    button.setMessage(Text.literal(this.armed ? "Armed: ON" : "Armed: OFF"));
                }
        ).dimensions(centerX - 60, startY + 100, 120, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Save"),
                button -> {
                    ClientPlayNetworking.send(new NoisemakerSaveSettingsPayload(
                            this.pos,
                            this.delaySeconds,
                            this.radius,
                            this.strength,
                            this.armed
                    ));

                    this.close();
                }
        ).dimensions(centerX - 105, startY + 132, 100, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Cancel"),
                button -> this.close()
        ).dimensions(centerX + 5, startY + 132, 100, 20).build());
    }

    private void addSettingButtons(
            int centerX,
            int y,
            String label,
            ValueText valueText,
            Runnable decrease,
            Runnable increase
    ) {
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("-"),
                button -> decrease.run()
        ).dimensions(centerX - 105, y, 24, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("+"),
                button -> increase.run()
        ).dimensions(centerX + 81, y, 24, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int startY = this.height / 2 - 70;

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, startY - 32, 0xFFFFFF);

        drawSetting(context, centerX, startY + 6, "Delay", this.delaySeconds + "s");
        drawSetting(context, centerX, startY + 38, "Radius", String.format("%.1f", this.radius));
        drawSetting(context, centerX, startY + 70, "Strength", String.format("%.2f", this.strength));

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("Position: " + this.pos.getX() + " " + this.pos.getY() + " " + this.pos.getZ()),
                centerX,
                startY + 164,
                0xAAAAAA
        );
    }

    private void drawSetting(DrawContext context, int centerX, int y, String label, String value) {
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal(label + ": " + value),
                centerX,
                y,
                0xFFFFFF
        );
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private interface ValueText {
        String get();
    }
}