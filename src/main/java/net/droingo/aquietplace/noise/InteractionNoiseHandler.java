package net.droingo.aquietplace.noise;

import net.droingo.aquietplace.AQuietPlace;
import net.droingo.aquietplace.config.QuietPlaceConfig;
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
        InteractionNoise interactionNoise = getInteractionNoise(blockState);

        if (interactionNoise == null) {
            return;
        }

        QuietPlaceConfig.InteractionNoise config = QuietPlaceConfig.get().interactionNoise;

        boolean quiet = player.isSneaking();

        float strength = interactionNoise.strength();
        float radius = interactionNoise.radius();
        float hudLevel = interactionNoise.hudLevel();

        if (quiet) {
            strength *= config.quietStrengthMultiplier;
            radius *= config.quietRadiusMultiplier;
            hudLevel *= config.quietStrengthMultiplier;
        }

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

    private static InteractionNoise getInteractionNoise(BlockState blockState) {
        QuietPlaceConfig.InteractionNoise config = QuietPlaceConfig.get().interactionNoise;
        Block block = blockState.getBlock();

        if (block instanceof DoorBlock) {
            boolean currentlyOpen = blockState.contains(Properties.OPEN) && blockState.get(Properties.OPEN);
            return new InteractionNoise(
                    currentlyOpen ? NoiseType.DOOR_CLOSE : NoiseType.DOOR_OPEN,
                    config.doorStrength,
                    config.doorRadius,
                    config.doorHudLevel
            );
        }

        if (block instanceof TrapdoorBlock) {
            boolean currentlyOpen = blockState.contains(Properties.OPEN) && blockState.get(Properties.OPEN);
            return new InteractionNoise(
                    currentlyOpen ? NoiseType.TRAPDOOR_CLOSE : NoiseType.TRAPDOOR_OPEN,
                    config.trapdoorStrength,
                    config.trapdoorRadius,
                    config.trapdoorHudLevel
            );
        }

        if (block instanceof FenceGateBlock) {
            boolean currentlyOpen = blockState.contains(Properties.OPEN) && blockState.get(Properties.OPEN);
            return new InteractionNoise(
                    currentlyOpen ? NoiseType.FENCE_GATE_CLOSE : NoiseType.FENCE_GATE_OPEN,
                    config.fenceGateStrength,
                    config.fenceGateRadius,
                    config.fenceGateHudLevel
            );
        }

        if (block instanceof ChestBlock || block instanceof BarrelBlock) {
            return new InteractionNoise(
                    NoiseType.CONTAINER_OPEN,
                    config.containerStrength,
                    config.containerRadius,
                    config.containerHudLevel
            );
        }

        if (block instanceof ButtonBlock) {
            return new InteractionNoise(
                    NoiseType.BUTTON_PRESS,
                    config.buttonStrength,
                    config.buttonRadius,
                    config.buttonHudLevel
            );
        }

        if (block instanceof LeverBlock) {
            return new InteractionNoise(
                    NoiseType.LEVER_TOGGLE,
                    config.leverStrength,
                    config.leverRadius,
                    config.leverHudLevel
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