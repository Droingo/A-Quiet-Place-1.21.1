package net.droingo.aquietplace.noise;

import net.droingo.aquietplace.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CarpetBlock;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class SoftSurfaceSystem {
    private static final float NORMAL_MULTIPLIER = 1.0f;
    private static final float WOOL_MULTIPLIER = 0.60f;
    private static final float CARPET_MULTIPLIER = 0.45f;
    private static final float SAND_MULTIPLIER = 0.70f;
    private static final float SAND_PATH_MULTIPLIER = 0.35f;

    private SoftSurfaceSystem() {
    }

    public static float getFootstepNoiseMultiplier(ServerWorld world, BlockPos playerBlockPos) {
        /*
         * Thin blocks like carpet and our Sand Path Layer can occupy the same block
         * position as the player's feet. Full blocks like wool/sand are usually below.
         *
         * Check feet first, then the block below.
         */
        BlockState feetState = world.getBlockState(playerBlockPos);
        float feetMultiplier = getFootstepNoiseMultiplier(feetState);

        if (feetMultiplier < NORMAL_MULTIPLIER) {
            return feetMultiplier;
        }

        BlockState belowState = world.getBlockState(playerBlockPos.down());
        return getFootstepNoiseMultiplier(belowState);
    }

    public static float getFootstepNoiseMultiplier(BlockState surfaceState) {
        Block block = surfaceState.getBlock();

        if (block == ModBlocks.SAND_PATH_LAYER) {
            return SAND_PATH_MULTIPLIER;
        }

        if (block instanceof CarpetBlock) {
            return CARPET_MULTIPLIER;
        }

        if (surfaceState.isIn(BlockTags.WOOL)) {
            return WOOL_MULTIPLIER;
        }

        if (block == Blocks.SAND || block == Blocks.RED_SAND) {
            return SAND_MULTIPLIER;
        }

        return NORMAL_MULTIPLIER;
    }

    public static boolean isSoftSurface(ServerWorld world, BlockPos playerBlockPos) {
        return getFootstepNoiseMultiplier(world, playerBlockPos) < NORMAL_MULTIPLIER;
    }
}