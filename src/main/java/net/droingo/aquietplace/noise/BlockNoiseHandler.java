package net.droingo.aquietplace.noise;

import net.droingo.aquietplace.AQuietPlace;
import net.droingo.aquietplace.config.QuietPlaceConfig;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

public final class BlockNoiseHandler {
    private BlockNoiseHandler() {
    }

    public static void register() {
        registerBlockBreakNoise();
        registerBlockPlaceNoise();

        AQuietPlace.LOGGER.info("Registered block noise handler");
    }

    private static void registerBlockBreakNoise() {
        PlayerBlockBreakEvents.AFTER.register((world, player, blockPos, blockState, blockEntity) -> {
            if (world.isClient()) {
                return;
            }

            if (!(world instanceof ServerWorld serverWorld)) {
                return;
            }

            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return;
            }

            emitBlockBreakNoise(serverWorld, serverPlayer, blockPos, blockState);
        });
    }

    private static void registerBlockPlaceNoise() {
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

            ItemStack heldStack = serverPlayer.getStackInHand(hand);

            if (!(heldStack.getItem() instanceof BlockItem)) {
                return ActionResult.PASS;
            }

            BlockPos targetPos = hitResult.getBlockPos().offset(hitResult.getSide());

            emitBlockPlaceNoise(serverWorld, serverPlayer, targetPos);

            return ActionResult.PASS;
        });
    }

    private static void emitBlockBreakNoise(
            ServerWorld world,
            ServerPlayerEntity player,
            BlockPos blockPos,
            BlockState brokenState
    ) {
        float strength = getBreakStrength(brokenState);
        float radius = getBreakRadius(brokenState);

        if (strength <= 0.0f || radius <= 0.0f) {
            return;
        }

        NoiseSystem.emitNoise(new NoiseEvent(
                world,
                blockPos.toCenterPos(),
                strength,
                radius,
                null,
                NoiseType.BLOCK_BREAK,
                true,
                true,
                world.getTime()
        ));

        PlayerNoiseHandler.addHudNoise(player, strength);
    }

    private static void emitBlockPlaceNoise(
            ServerWorld world,
            ServerPlayerEntity player,
            BlockPos placePos
    ) {
        QuietPlaceConfig.BlockNoise config = QuietPlaceConfig.get().blockNoise;

        float strength = config.placeStrength;
        float radius = config.placeRadius;
        float hudLevel = config.placeHudLevel;

        if (player.isSneaking()) {
            strength *= config.quietPlaceStrengthMultiplier;
            radius *= config.quietPlaceRadiusMultiplier;
            hudLevel *= config.quietPlaceStrengthMultiplier;
        }

        NoiseSystem.emitNoise(new NoiseEvent(
                world,
                placePos.toCenterPos(),
                strength,
                radius,
                null,
                NoiseType.BLOCK_PLACE,
                true,
                true,
                world.getTime()
        ));

        PlayerNoiseHandler.addHudNoise(player, hudLevel);
    }

    private static float getBreakStrength(BlockState blockState) {
        QuietPlaceConfig.BlockNoise config = QuietPlaceConfig.get().blockNoise;
        float hardness = blockState.getHardness(null, BlockPos.ORIGIN);

        if (hardness < 0.0f) {
            return 0.0f;
        }

        if (hardness >= config.hardHardnessThreshold) {
            return config.hardBreakStrength;
        }

        if (hardness >= config.mediumHardnessThreshold) {
            return config.mediumBreakStrength;
        }

        return config.softBreakStrength;
    }

    private static float getBreakRadius(BlockState blockState) {
        QuietPlaceConfig.BlockNoise config = QuietPlaceConfig.get().blockNoise;
        float hardness = blockState.getHardness(null, BlockPos.ORIGIN);

        if (hardness < 0.0f) {
            return 0.0f;
        }

        if (hardness >= config.hardHardnessThreshold) {
            return config.hardBreakRadius;
        }

        if (hardness >= config.mediumHardnessThreshold) {
            return config.mediumBreakRadius;
        }

        return config.softBreakRadius;
    }
}