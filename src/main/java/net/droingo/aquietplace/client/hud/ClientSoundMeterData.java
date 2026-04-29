package net.droingo.aquietplace.client.hud;

public final class ClientSoundMeterData {
    private static long visibleUntilMillis = 0L;

    private static int ambientLevelOrdinal = 0;
    private static int ambientStrengthPercent = 0;
    private static int maskingPercent = 0;
    private static int sourceFlags = 0;

    private ClientSoundMeterData() {
    }

    public static void show(
            int newAmbientLevelOrdinal,
            int newAmbientStrengthPercent,
            int newMaskingPercent,
            int newSourceFlags,
            int displayTicks
    ) {
        if (displayTicks <= 0) {
            hide();
            return;
        }

        ambientLevelOrdinal = newAmbientLevelOrdinal;
        ambientStrengthPercent = clampPercent(newAmbientStrengthPercent);
        maskingPercent = clampPercent(newMaskingPercent);
        sourceFlags = newSourceFlags;

        visibleUntilMillis = System.currentTimeMillis() + displayTicks * 50L;
    }

    public static void hide() {
        visibleUntilMillis = 0L;
    }

    public static void tick() {
        // Intentionally empty.
        // The HUD render callback can run faster than 20 FPS, so we use real time instead.
    }

    public static boolean shouldRender() {
        return System.currentTimeMillis() < visibleUntilMillis;
    }

    public static int getAmbientLevelOrdinal() {
        return ambientLevelOrdinal;
    }

    public static int getAmbientStrengthPercent() {
        return ambientStrengthPercent;
    }

    public static int getMaskingPercent() {
        return maskingPercent;
    }

    public static int getSourceFlags() {
        return sourceFlags;
    }

    public static float getFadeProgress() {
        long remainingMillis = visibleUntilMillis - System.currentTimeMillis();

        if (remainingMillis >= 1000L) {
            return 1.0f;
        }

        if (remainingMillis <= 0L) {
            return 0.0f;
        }

        return remainingMillis / 1000.0f;
    }

    private static int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }
}