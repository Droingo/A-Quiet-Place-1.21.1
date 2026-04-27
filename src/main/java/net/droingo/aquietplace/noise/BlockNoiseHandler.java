package net.droingo.aquietplace.noise;

import net.droingo.aquietplace.AQuietPlace;
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
    private static final float QUIET_PLACE_RADIUS_MULTIPLIER = 0.35f;
    private static final float QUIET_PLACE_STRENGTH_MULTIPLIER = 0.50f;

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
        float strength = 0.45f;
        float radius = 7.0f;
        float hudLevel = 0.45f;

        if (player.isSneaking()) {
            strength *= QUIET_PLACE_STRENGTH_MULTIPLIER;
            radius *= QUIET_PLACE_RADIUS_MULTIPLIER;
            hudLevel *= QUIET_PLACE_STRENGTH_MULTIPLIER;
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
        float hardness = blockState.getHardness(null, BlockPos.ORIGIN);

        if (hardness < 0.0f) {
            return 0.0f;
        }

        if (hardness >= 5.0f) {
            return 0.95f;
        }

        if (hardness >= 2.0f) {
            return 0.75f;
        }

        return 0.60f;
    }

    private static float getBreakRadius(BlockState blockState) {
        float hardness = blockState.getHardness(null, BlockPos.ORIGIN);

        if (hardness < 0.0f) {
            return 0.0f;
        }

        if (hardness >= 5.0f) {
            return 22.0f;
        }

        if (hardness >= 2.0f) {
            return 16.0f;
        }

        return 12.0f;
    }
}