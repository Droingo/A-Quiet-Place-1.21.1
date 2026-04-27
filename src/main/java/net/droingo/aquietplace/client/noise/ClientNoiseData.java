package net.droingo.aquietplace.client.noise;

public final class ClientNoiseData {
    private static float noiseLevel;

    private ClientNoiseData() {
    }

    public static float getNoiseLevel() {
        return noiseLevel;
    }

    public static void setNoiseLevel(float level) {
        noiseLevel = Math.max(0.0f, Math.min(1.0f, level));
    }
}