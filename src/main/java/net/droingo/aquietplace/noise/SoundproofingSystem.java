package net.droingo.aquietplace.noise;

import net.droingo.aquietplace.config.QuietPlaceConfig;
import net.droingo.aquietplace.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class SoundproofingSystem {
    private SoundproofingSystem() {
    }

    public static NoiseEvent applySoundproofing(NoiseEvent originalEvent) {
        QuietPlaceConfig.Soundproofing config = QuietPlaceConfig.get().soundproofing;

        if (!config.enabled) {
            return originalEvent;
        }

        if (!(originalEvent.world() instanceof ServerWorld world)) {
            return originalEvent;
        }

        if (originalEvent.radius() <= 0.0f || originalEvent.strength() <= 0.0f) {
            return originalEvent;
        }

        BlockPos centerPos = BlockPos.ofFloored(originalEvent.position());

        int panelCount = countNearbyPanels(world, centerPos, config.scanRadius);

        if (panelCount <= 0) {
            return originalEvent;
        }

        float radiusMultiplier = calculateMultiplier(
                panelCount,
                config.radiusReductionPerPanel,
                config.maxRadiusReduction,
                config.minimumRadiusMultiplier
        );

        float strengthMultiplier = calculateMultiplier(
                panelCount,
                config.strengthReductionPerPanel,
                config.maxStrengthReduction,
                config.minimumStrengthMultiplier
        );

        float reducedStrength = Math.max(
                config.minimumFinalStrength,
                originalEvent.strength() * strengthMultiplier
        );

        float reducedRadius = Math.max(
                config.minimumFinalRadius,
                originalEvent.radius() * radiusMultiplier
        );

        return new NoiseEvent(
                originalEvent.world(),
                originalEvent.position(),
                reducedStrength,
                reducedRadius,
                originalEvent.sourceEntityUuid(),
                originalEvent.type(),
                originalEvent.visibleOnHud(),
                originalEvent.attractsDeathAngels(),
                originalEvent.gameTime()
        );
    }

    private static int countNearbyPanels(ServerWorld world, BlockPos centerPos, int scanRadius) {
        QuietPlaceConfig.Soundproofing config = QuietPlaceConfig.get().soundproofing;

        int maxUsefulPanels = getMaxUsefulPanelCount(config);
        int foundPanels = 0;

        BlockPos.Mutable mutablePos = new BlockPos.Mutable();

        for (int x = -scanRadius; x <= scanRadius; x++) {
            for (int y = -scanRadius; y <= scanRadius; y++) {
                for (int z = -scanRadius; z <= scanRadius; z++) {
                    mutablePos.set(
                            centerPos.getX() + x,
                            centerPos.getY() + y,
                            centerPos.getZ() + z
                    );

                    BlockState blockState = world.getBlockState(mutablePos);

                    if (blockState.isOf(ModBlocks.NEWSPAPER_SOUNDPROOFING)) {
                        foundPanels++;

                        /*
                         * Early exit:
                         * Once we have enough panels to hit max reduction,
                         * scanning more blocks would not change the result.
                         */
                        if (foundPanels >= maxUsefulPanels) {
                            return foundPanels;
                        }
                    }
                }
            }
        }

        return foundPanels;
    }

    private static int getMaxUsefulPanelCount(QuietPlaceConfig.Soundproofing config) {
        float strongestReductionPerPanel = Math.max(
                config.radiusReductionPerPanel,
                config.strengthReductionPerPanel
        );

        float largestMaxReduction = Math.max(
                config.maxRadiusReduction,
                config.maxStrengthReduction
        );

        if (strongestReductionPerPanel <= 0.0f) {
            return 1;
        }

        return Math.max(1, (int) Math.ceil(largestMaxReduction / strongestReductionPerPanel));
    }

    private static float calculateMultiplier(
            int panelCount,
            float reductionPerPanel,
            float maxReduction,
            float minimumMultiplier
    ) {
        float reduction = Math.min(maxReduction, panelCount * reductionPerPanel);
        float multiplier = 1.0f - reduction;

        return Math.max(minimumMultiplier, multiplier);
    }
}