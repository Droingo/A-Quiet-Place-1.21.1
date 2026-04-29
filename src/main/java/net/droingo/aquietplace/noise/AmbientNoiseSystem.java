package net.droingo.aquietplace.noise;

import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.droingo.aquietplace.registry.ModBlocks;

public final class AmbientNoiseSystem {
    private static final int HORIZONTAL_SCAN_RADIUS = 6;
    private static final int VERTICAL_SCAN_RADIUS = 3;

    private static final int WATERFALL_NOISE_RADIUS = 16;
    private static final float NATURAL_AMBIENT_STRENGTH_CAP = 0.85f;

    private AmbientNoiseSystem() {
    }

    public static AmbientNoiseReading scan(ServerWorld world, BlockPos centerPos) {
        int waterScore = 0;
        int lavaScore = 0;

        boolean waterfallNoiseBlockNearby = false;

        BlockPos min = centerPos.add(
                -HORIZONTAL_SCAN_RADIUS,
                -VERTICAL_SCAN_RADIUS,
                -HORIZONTAL_SCAN_RADIUS
        );

        BlockPos max = centerPos.add(
                HORIZONTAL_SCAN_RADIUS,
                VERTICAL_SCAN_RADIUS,
                HORIZONTAL_SCAN_RADIUS
        );

        for (BlockPos scanPos : BlockPos.iterate(min, max)) {
            double distanceSquared = scanPos.getSquaredDistance(centerPos);

            int weight;
            if (distanceSquared <= 9.0) {
                weight = 3;
            } else if (distanceSquared <= 36.0) {
                weight = 2;
            } else {
                weight = 1;
            }

            if (world.getFluidState(scanPos).isIn(FluidTags.WATER)) {
                waterScore += weight;
            }

            if (world.getBlockState(scanPos).isOf(ModBlocks.WATERFALL_NOISE_BLOCK)) {
                waterfallNoiseBlockNearby = true;
            }

            if (world.getFluidState(scanPos).isIn(FluidTags.LAVA)) {
                lavaScore += weight;
            }
        }

        float waterStrength = clamp01(waterScore / 90.0f);
        float lavaStrength = clamp01(lavaScore / 70.0f);

        float weatherStrength = 0.0f;
        if (world.isRaining() && world.hasRain(centerPos.up())) {
            weatherStrength += 0.18f;
        }

        if (world.isThundering() && world.hasRain(centerPos.up())) {
            weatherStrength += 0.22f;
        }

        float naturalStrength = Math.min(
                NATURAL_AMBIENT_STRENGTH_CAP,
                clamp01(waterStrength + lavaStrength + weatherStrength)
        );

        float totalStrength = waterfallNoiseBlockNearby ? 1.0f : naturalStrength;

        AmbientNoiseLevel level = AmbientNoiseLevel.fromStrength(totalStrength);

        return new AmbientNoiseReading(
                centerPos.toCenterPos(),
                totalStrength,
                level,
                waterScore,
                lavaScore,
                weatherStrength,
                waterfallNoiseBlockNearby,
                getNoiseMaskingMultiplier(totalStrength, waterfallNoiseBlockNearby)
        );
    }

    public static float getNoiseMaskingMultiplier(float ambientStrength) {
        return getNoiseMaskingMultiplier(ambientStrength, false);
    }

    public static float getNoiseMaskingMultiplier(float ambientStrength, boolean waterfallNoiseBlockNearby) {
        if (waterfallNoiseBlockNearby) {
            return 0.0f;
        }

        /*
         * Natural ambient noise can help a lot, but it never gives perfect protection.
         * 0.0 ambient = normal noise.
         * 0.85 natural ambient = noise radius reduced by about 51%.
         */
        return 1.0f - (clamp01(ambientStrength) * 0.60f);
    }

    private static float clamp01(float value) {
        if (value < 0.0f) {
            return 0.0f;
        }

        if (value > 1.0f) {
            return 1.0f;
        }

        return value;
    }

    public enum AmbientNoiseLevel {
        SILENT("Silent"),
        LOW("Low"),
        MODERATE("Moderate"),
        HIGH("High"),
        DEAFENING("Deafening");

        private final String displayName;

        AmbientNoiseLevel(String displayName) {
            this.displayName = displayName;
        }

        public Text asText() {
            return Text.literal(this.displayName);
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public static AmbientNoiseLevel fromStrength(float strength) {
            if (strength >= 0.80f) {
                return DEAFENING;
            }

            if (strength >= 0.55f) {
                return HIGH;
            }

            if (strength >= 0.30f) {
                return MODERATE;
            }

            if (strength >= 0.10f) {
                return LOW;
            }

            return SILENT;
        }
    }

    public record AmbientNoiseReading(
            Vec3d position,
            float strength,
            AmbientNoiseLevel level,
            int waterScore,
            int lavaScore,
            float weatherStrength,
            boolean waterfallNoiseBlockNearby,
            float noiseMaskingMultiplier
    ){
        public int strengthPercent() {
            return Math.round(this.strength * 100.0f);
        }

        public int maskingPercent() {
            return Math.round((1.0f - this.noiseMaskingMultiplier) * 100.0f);
        }
    }
}