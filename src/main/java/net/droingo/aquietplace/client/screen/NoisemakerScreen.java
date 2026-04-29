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
    private final boolean active;
    private final int countdownTicks;

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
        this.active = payload.active();
        this.countdownTicks = payload.countdownTicks();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - 78;

        if (!this.active) {
            addSettingButtons(
                    centerX,
                    startY,
                    () -> this.delaySeconds = Math.max(0, this.delaySeconds - 1),
                    () -> this.delaySeconds = Math.min(60, this.delaySeconds + 1)
            );

            addSettingButtons(
                    centerX,
                    startY + 32,
                    () -> this.radius = Math.max(1.0f, this.radius - 1.0f),
                    () -> this.radius = Math.min(128.0f, this.radius + 1.0f)
            );

            addSettingButtons(
                    centerX,
                    startY + 64,
                    () -> this.strength = Math.max(0.05f, this.strength - 0.05f),
                    () -> this.strength = Math.min(1.0f, this.strength + 0.05f)
            );

            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal(this.armed ? "Start Armed: ON" : "Start Armed: OFF"),
                    button -> {
                        this.armed = !this.armed;
                        button.setMessage(Text.literal(this.armed ? "Start Armed: ON" : "Start Armed: OFF"));
                    }
            ).dimensions(centerX - 70, startY + 100, 140, 20).build());

            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Save"),
                    button -> saveAndClose(this.armed)
            ).dimensions(centerX - 105, startY + 132, 100, 20).build());

            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Cancel"),
                    button -> this.close()
            ).dimensions(centerX + 5, startY + 132, 100, 20).build());

            return;
        }

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Disarm"),
                button -> saveAndClose(false)
        ).dimensions(centerX - 105, startY + 132, 100, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Close"),
                button -> this.close()
        ).dimensions(centerX + 5, startY + 132, 100, 20).build());
    }

    private void addSettingButtons(
            int centerX,
            int y,
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

    private void saveAndClose(boolean armedValue) {
        ClientPlayNetworking.send(new NoisemakerSaveSettingsPayload(
                this.pos,
                this.delaySeconds,
                this.radius,
                this.strength,
                armedValue
        ));

        this.close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int startY = this.height / 2 - 78;

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, startY - 32, 0xFFFFFF);

        drawSetting(context, centerX, startY + 6, "Delay", this.delaySeconds + "s");
        drawSetting(context, centerX, startY + 38, "Radius", String.format("%.1f", this.radius));
        drawSetting(context, centerX, startY + 70, "Strength", String.format("%.2f", this.strength));

        if (this.active) {
            int secondsRemaining = Math.max(0, (int) Math.ceil(this.countdownTicks / 20.0));
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.literal("ACTIVE - " + secondsRemaining + "s remaining"),
                    centerX,
                    startY + 104,
                    0xFF5555
            );

            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.literal("Settings are locked while active."),
                    centerX,
                    startY + 118,
                    0xAAAAAA
            );
        } else {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.literal("Redstone power can also start the countdown."),
                    centerX,
                    startY + 164,
                    0xAAAAAA
            );
        }

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("Position: " + this.pos.getX() + " " + this.pos.getY() + " " + this.pos.getZ()),
                centerX,
                startY + 184,
                0x777777
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
}