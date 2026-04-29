package net.droingo.aquietplace.client.gui;

import net.droingo.aquietplace.network.GlassBottleTrapDisarmClickPayload;
import net.droingo.aquietplace.network.GlassBottleTrapDisarmReadyPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

public class GlassBottleTrapDisarmScreen extends Screen {
    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 24;

    private static final int ACTIVE_WINDOW_TICKS = 8;
    private static final int HIDDEN_WINDOW_TICKS = 5;

    private final BlockPos trapPos;
    private final int requiredClicks;
    private final int timeLimitTicks;
    private final Random random = Random.create();

    private boolean started = false;
    private long startMillis = 0L;
    private long endMillis = 0L;

    private int localClicks = 0;
    private int phaseTicks = 0;
    private boolean takeButtonActive = false;

    private ButtonWidget readyButton;
    private ButtonWidget takeBottleButton;

    public GlassBottleTrapDisarmScreen(BlockPos trapPos, int requiredClicks, int timeLimitTicks) {
        super(Text.literal("Disarm Glass Bottle Trap"));

        this.trapPos = trapPos;
        this.requiredClicks = requiredClicks;
        this.timeLimitTicks = timeLimitTicks;
    }

    @Override
    protected void init() {
        this.readyButton = ButtonWidget.builder(
                Text.literal("Ready"),
                button -> startQte()
        ).dimensions(
                this.width / 2 - BUTTON_WIDTH / 2,
                this.height / 2,
                BUTTON_WIDTH,
                BUTTON_HEIGHT
        ).build();

        this.takeBottleButton = ButtonWidget.builder(
                Text.literal("Take Bottle"),
                button -> clickQteButton()
        ).dimensions(
                this.width / 2 - BUTTON_WIDTH / 2,
                this.height / 2,
                BUTTON_WIDTH,
                BUTTON_HEIGHT
        ).build();

        this.addDrawableChild(this.readyButton);
        this.addDrawableChild(this.takeBottleButton);

        this.readyButton.visible = true;
        this.readyButton.active = true;

        setTakeButtonActive(false);
    }

    private void startQte() {
        this.started = true;
        this.startMillis = System.currentTimeMillis();
        this.endMillis = this.startMillis + (this.timeLimitTicks * 50L);

        this.readyButton.visible = false;
        this.readyButton.active = false;

        ClientPlayNetworking.send(new GlassBottleTrapDisarmReadyPayload(this.trapPos));

        moveTakeButtonToRandomPosition();
        this.phaseTicks = 0;
        setTakeButtonActive(true);
    }

    private void clickQteButton() {
        if (!this.started || !this.takeButtonActive) {
            return;
        }

        if (System.currentTimeMillis() > this.endMillis) {
            this.close();
            return;
        }

        this.localClicks++;
        ClientPlayNetworking.send(new GlassBottleTrapDisarmClickPayload(this.trapPos));

        moveTakeButtonToRandomPosition();
        this.phaseTicks = 0;
        setTakeButtonActive(true);

        if (this.localClicks >= this.requiredClicks) {
            this.close();
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.started) {
            return;
        }

        if (System.currentTimeMillis() > this.endMillis) {
            this.close();
            return;
        }

        this.phaseTicks++;

        if (this.takeButtonActive) {
            if (this.phaseTicks >= ACTIVE_WINDOW_TICKS) {
                this.phaseTicks = 0;
                setTakeButtonActive(false);
            }
        } else {
            if (this.phaseTicks >= HIDDEN_WINDOW_TICKS) {
                this.phaseTicks = 0;
                moveTakeButtonToRandomPosition();
                setTakeButtonActive(true);
            }
        }
    }

    private void setTakeButtonActive(boolean active) {
        this.takeButtonActive = active;
        this.takeBottleButton.visible = active;
        this.takeBottleButton.active = active;
    }

    private void moveTakeButtonToRandomPosition() {
        int minX = 20;
        int maxX = Math.max(minX, this.width - BUTTON_WIDTH - 20);

        int minY = Math.max(50, this.height / 2 - 40);
        int maxY = Math.max(minY, this.height - BUTTON_HEIGHT - 60);

        int x = MathHelper.nextInt(this.random, minX, maxX);
        int y = MathHelper.nextInt(this.random, minY, maxY);

        this.takeBottleButton.setPosition(x, y);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x55000000);

        int centerX = this.width / 2;
        int titleY = 40;

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("Disarm the Glass Bottle Trap"),
                centerX,
                titleY,
                0xFFFFFF
        );

        if (!this.started) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.literal("Press Ready to begin."),
                    centerX,
                    titleY + 18,
                    0xFFD966
            );

            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.literal("A Take Bottle button will appear in random spots."),
                    centerX,
                    titleY + 36,
                    0xDDDDDD
            );

            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.literal("Click it 3 times quickly before time runs out."),
                    centerX,
                    titleY + 50,
                    0xDDDDDD
            );

            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.literal("Failing or moving away will trigger the trap."),
                    centerX,
                    titleY + 64,
                    0xFF8888
            );
        } else {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.literal("Grab the bottle: " + this.localClicks + " / " + this.requiredClicks),
                    centerX,
                    titleY + 18,
                    0xFFD966
            );

            drawTimeBar(context);

            if (!this.takeButtonActive) {
                context.drawCenteredTextWithShadow(
                        this.textRenderer,
                        Text.literal("Wait for an opening..."),
                        centerX,
                        this.height / 2 + 40,
                        0xFF8888
                );
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawTimeBar(DrawContext context) {
        long now = System.currentTimeMillis();
        long total = Math.max(1L, this.endMillis - this.startMillis);
        long remaining = Math.max(0L, this.endMillis - now);

        float progress = remaining / (float) total;

        int barWidth = 160;
        int barHeight = 8;
        int x = (this.width - barWidth) / 2;
        int y = 75;

        context.fill(x, y, x + barWidth, y + barHeight, 0xFF222222);

        int filledWidth = (int) (barWidth * progress);
        int color = progress > 0.5f ? 0xFF55FF55 : progress > 0.25f ? 0xFFFFFF55 : 0xFFFF5555;

        context.fill(x, y, x + filledWidth, y + barHeight, color);
    }

    @Override
    protected void applyBlur(float delta) {
        // Disable Minecraft's menu blur for this QTE screen.
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}