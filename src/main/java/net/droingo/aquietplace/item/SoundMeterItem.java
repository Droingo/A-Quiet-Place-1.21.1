package net.droingo.aquietplace.item;

import net.droingo.aquietplace.network.SoundMeterScanPayload;
import net.droingo.aquietplace.noise.AmbientNoiseSystem;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SoundMeterItem extends Item {
    private static final int REFRESH_INTERVAL_TICKS = 20;
    private static final int HUD_DISPLAY_TICKS = 80;
    private static final int SWITCH_AWAY_FADE_TICKS = 8;

    private static final Set<UUID> ACTIVE_PLAYERS = new HashSet<>();

    public SoundMeterItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (!(world instanceof ServerWorld serverWorld) || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return TypedActionResult.success(stack);
        }

        UUID playerUuid = serverPlayer.getUuid();

        if (ACTIVE_PLAYERS.contains(playerUuid)) {
            ACTIVE_PLAYERS.remove(playerUuid);
            hideMeter(serverPlayer);
            player.sendMessage(Text.literal("Sound Meter disabled."), true);

            serverWorld.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(),
                    SoundCategory.PLAYERS,
                    0.25f,
                    0.8f
            );
        } else {
            ACTIVE_PLAYERS.add(playerUuid);
            sendScan(serverWorld, serverPlayer, HUD_DISPLAY_TICKS);
            player.sendMessage(Text.literal("Sound Meter enabled."), true);

            serverWorld.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(),
                    SoundCategory.PLAYERS,
                    0.35f,
                    1.8f
            );
        }

        player.getItemCooldownManager().set(this, 10);

        return TypedActionResult.success(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }

        if (!(entity instanceof ServerPlayerEntity player)) {
            return;
        }

        UUID playerUuid = player.getUuid();

        if (!ACTIVE_PLAYERS.contains(playerUuid)) {
            return;
        }

        if (!isHoldingThisMeter(player, stack)) {
            ACTIVE_PLAYERS.remove(playerUuid);
            sendQuickFade(serverWorld, player);
            player.sendMessage(Text.literal("Sound Meter disabled."), true);
            return;
        }

        if (world.getTime() % REFRESH_INTERVAL_TICKS != 0) {
            return;
        }

        sendScan(serverWorld, player, HUD_DISPLAY_TICKS);
    }

    private boolean isHoldingThisMeter(ServerPlayerEntity player, ItemStack stack) {
        return player.getMainHandStack() == stack || player.getOffHandStack() == stack;
    }
    private void sendQuickFade(ServerWorld world, ServerPlayerEntity player) {
        AmbientNoiseSystem.AmbientNoiseReading reading = AmbientNoiseSystem.scan(
                world,
                player.getBlockPos()
        );

        int sourceFlags = 0;

        if (reading.waterScore() > 0) {
            sourceFlags |= SoundMeterScanPayload.SOURCE_WATER;
        }

        if (reading.lavaScore() > 0) {
            sourceFlags |= SoundMeterScanPayload.SOURCE_LAVA;
        }

        if (reading.weatherStrength() > 0.0f) {
            sourceFlags |= SoundMeterScanPayload.SOURCE_WEATHER;
        }
        if (reading.waterfallNoiseBlockNearby()) {
            sourceFlags |= SoundMeterScanPayload.SOURCE_WATERFALL;
        }

        ServerPlayNetworking.send(
                player,
                new SoundMeterScanPayload(
                        reading.level().ordinal(),
                        reading.strengthPercent(),
                        reading.maskingPercent(),
                        sourceFlags,
                        SWITCH_AWAY_FADE_TICKS
                )
        );
    }

    private void sendScan(ServerWorld world, ServerPlayerEntity player, int displayTicks) {
        AmbientNoiseSystem.AmbientNoiseReading reading = AmbientNoiseSystem.scan(
                world,
                player.getBlockPos()
        );

        int sourceFlags = 0;

        if (reading.waterScore() > 0) {
            sourceFlags |= SoundMeterScanPayload.SOURCE_WATER;
        }

        if (reading.lavaScore() > 0) {
            sourceFlags |= SoundMeterScanPayload.SOURCE_LAVA;
        }

        if (reading.weatherStrength() > 0.0f) {
            sourceFlags |= SoundMeterScanPayload.SOURCE_WEATHER;
        }
        if (reading.waterfallNoiseBlockNearby()) {
            sourceFlags |= SoundMeterScanPayload.SOURCE_WATERFALL;
        }

        ServerPlayNetworking.send(
                player,
                new SoundMeterScanPayload(
                        reading.level().ordinal(),
                        reading.strengthPercent(),
                        reading.maskingPercent(),
                        sourceFlags,
                        displayTicks
                )
        );
    }

    private void hideMeter(ServerPlayerEntity player) {
        ServerPlayNetworking.send(
                player,
                new SoundMeterScanPayload(
                        0,
                        0,
                        0,
                        0,
                        0
                )
        );
    }
}