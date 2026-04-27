package net.droingo.aquietplace.noise;

import net.droingo.aquietplace.AQuietPlace;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

public final class InteractionNoiseHandler {
    private static final float QUIET_INTERACTION_RADIUS_MULTIPLIER = 0.20f;
    private static final float QUIET_INTERACTION_STRENGTH_MULTIPLIER = 0.35f;

    private InteractionNoiseHandler() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient()) {
                return ActionResult.PASS;
            }

            if (hand != Hand.MAIN_HAND) {
                return ActionResult.PASS;
            }

            if (!(world instanceof ServerWorld serverWorld)) {
                return ActionResult.PASS;
            }

            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }

            BlockPos blockPos = hitResult.getBlockPos();
            BlockState blockState = serverWorld.getBlockState(blockPos);

            emitInteractionNoise(serverWorld, serverPlayer, blockPos, blockState);

            return ActionResult.PASS;
        });

        AQuietPlace.LOGGER.info("Registered interaction noise handler");
    }

    private static void emitInteractionNoise(
            ServerWorld world,
            ServerPlayerEntity player,
            BlockPos blockPos,
            BlockState blockState
    ) {
        Block block = blockState.getBlock();

        InteractionNoise interactionNoise = getInteractionNoise(block, blockState);

        if (interactionNoise == null) {
            return;
        }

        boolean quiet = player.isSneaking();

        float strength = interactionNoise.strength();
        float radius = interactionNoise.radius();
        float hudLevel = interactionNoise.hudLevel();

        if (quiet) {
            strength *= QUIET_INTERACTION_STRENGTH_MULTIPLIER;
            radius *= QUIET_INTERACTION_RADIUS_MULTIPLIER;
            hudLevel *= QUIET_INTERACTION_STRENGTH_MULTIPLIER;
        }

        /*
         * Important:
         * Interaction noises are emitted from the block position with no source entity.
         *
         * This means a loud door/chest makes the Death Angel investigate the block,
         * but it does not immediately become a perfect player chase like sprinting does.
         */
        NoiseSystem.emitNoise(new NoiseEvent(
                world,
                blockPos.toCenterPos(),
                strength,
                radius,
                null,
                interactionNoise.noiseType(),
                true,
                true,
                world.getTime()
        ));

        PlayerNoiseHandler.addHudNoise(player, hudLevel);
    }

    private static InteractionNoise getInteractionNoise(Block block, BlockState blockState) {
        if (block instanceof DoorBlock) {
            boolean currentlyOpen = blockState.contains(Properties.OPEN) && blockState.get(Properties.OPEN);
            return new InteractionNoise(
                    currentlyOpen ? NoiseType.DOOR_CLOSE : NoiseType.DOOR_OPEN,
                    0.80f,
                    14.0f,
                    0.70f
            );
        }

        if (block instanceof TrapdoorBlock) {
            boolean currentlyOpen = blockState.contains(Properties.OPEN) && blockState.get(Properties.OPEN);
            return new InteractionNoise(
                    currentlyOpen ? NoiseType.TRAPDOOR_CLOSE : NoiseType.TRAPDOOR_OPEN,
                    0.65f,
                    11.0f,
                    0.60f
            );
        }

        if (block instanceof FenceGateBlock) {
            boolean currentlyOpen = blockState.contains(Properties.OPEN) && blockState.get(Properties.OPEN);
            return new InteractionNoise(
                    currentlyOpen ? NoiseType.FENCE_GATE_CLOSE : NoiseType.FENCE_GATE_OPEN,
                    0.65f,
                    11.0f,
                    0.60f
            );
        }

        if (block instanceof ChestBlock || block instanceof BarrelBlock) {
            return new InteractionNoise(
                    NoiseType.CONTAINER_OPEN,
                    0.55f,
                    9.0f,
                    0.55f
            );
        }

        if (block instanceof ButtonBlock) {
            return new InteractionNoise(
                    NoiseType.BUTTON_PRESS,
                    0.45f,
                    7.0f,
                    0.45f
            );
        }

        if (block instanceof LeverBlock) {
            return new InteractionNoise(
                    NoiseType.LEVER_TOGGLE,
                    0.55f,
                    9.0f,
                    0.55f
            );
        }

        return null;
    }

    private record InteractionNoise(
            NoiseType noiseType,
            float strength,
            float radius,
            float hudLevel
    ) {
    }
}