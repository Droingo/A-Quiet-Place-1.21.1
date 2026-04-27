package net.droingo.aquietplace.noise;

import net.droingo.aquietplace.AQuietPlace;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ActionNoiseHandler {
    private static final Map<UUID, PlayerActionState> PLAYER_STATES = new HashMap<>();
    private static final Set<UUID> KNOWN_DROPPED_ITEM_UUIDS = new HashSet<>();

    private static final int EATING_NOISE_INTERVAL_TICKS = 12;
    private static final int DROP_SCAN_INTERVAL_TICKS = 5;
    private static final double DROPPED_ITEM_SCAN_RADIUS = 24.0;

    private ActionNoiseHandler() {
    }

    public static void register() {
        registerAttackNoise();
        ServerTickEvents.END_SERVER_TICK.register(ActionNoiseHandler::tickServer);

        AQuietPlace.LOGGER.info("Registered action noise handler");
    }

    private static void registerAttackNoise() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
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

            if (player.isSpectator()) {
                return ActionResult.PASS;
            }

            emitPlayerActionNoise(
                    serverWorld,
                    serverPlayer,
                    player.getPos(),
                    NoiseType.ATTACK,
                    0.85f,
                    14.0f
            );

            return ActionResult.PASS;
        });
    }

    private static void tickServer(MinecraftServer server) {
        Set<UUID> onlinePlayerUuids = new HashSet<>();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            onlinePlayerUuids.add(player.getUuid());
            tickPlayer(player);
        }

        PLAYER_STATES.keySet().removeIf(uuid -> !onlinePlayerUuids.contains(uuid));

        scanDroppedItems(server);
    }

    private static void tickPlayer(ServerPlayerEntity player) {
        if (player.isSpectator()) {
            return;
        }

        PlayerActionState state = PLAYER_STATES.computeIfAbsent(
                player.getUuid(),
                uuid -> new PlayerActionState(player.getHealth())
        );

        handleDamageNoise(player, state);
        handleEatingOrDrinkingNoise(player, state);

        state.lastHealth = player.getHealth();

        if (state.eatingNoiseCooldownTicks > 0) {
            state.eatingNoiseCooldownTicks--;
        }
    }

    private static void handleDamageNoise(ServerPlayerEntity player, PlayerActionState state) {
        float currentHealth = player.getHealth();

        if (currentHealth >= state.lastHealth) {
            return;
        }

        float lostHealth = state.lastHealth - currentHealth;

        float strength = Math.min(1.0f, 0.55f + lostHealth * 0.08f);
        float radius = Math.min(24.0f, 9.0f + lostHealth * 2.0f);

        emitPlayerActionNoise(
                player.getServerWorld(),
                player,
                player.getPos(),
                NoiseType.DAMAGE,
                strength,
                radius
        );
    }

    private static void handleEatingOrDrinkingNoise(ServerPlayerEntity player, PlayerActionState state) {
        if (!player.isUsingItem()) {
            return;
        }

        ItemStack activeStack = player.getActiveItem();

        if (activeStack.isEmpty()) {
            return;
        }

        UseAction useAction = activeStack.getUseAction();

        if (useAction != UseAction.EAT && useAction != UseAction.DRINK) {
            return;
        }

        if (state.eatingNoiseCooldownTicks > 0) {
            return;
        }

        NoiseType noiseType = useAction == UseAction.DRINK ? NoiseType.EAT : NoiseType.EAT;

        emitPlayerActionNoise(
                player.getServerWorld(),
                player,
                player.getPos(),
                noiseType,
                0.35f,
                5.0f
        );

        state.eatingNoiseCooldownTicks = EATING_NOISE_INTERVAL_TICKS;
    }

    private static void scanDroppedItems(MinecraftServer server) {
        if (server.getTicks() % DROP_SCAN_INTERVAL_TICKS != 0) {
            return;
        }

        Set<UUID> stillExistingDroppedItems = new HashSet<>();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.isSpectator()) {
                continue;
            }

            ServerWorld world = player.getServerWorld();
            Box scanBox = player.getBoundingBox().expand(DROPPED_ITEM_SCAN_RADIUS);

            for (ItemEntity itemEntity : world.getEntitiesByClass(ItemEntity.class, scanBox, Entity::isAlive)) {
                UUID itemUuid = itemEntity.getUuid();
                stillExistingDroppedItems.add(itemUuid);

                if (KNOWN_DROPPED_ITEM_UUIDS.contains(itemUuid)) {
                    continue;
                }

                KNOWN_DROPPED_ITEM_UUIDS.add(itemUuid);

                Entity owner = itemEntity.getOwner();

                if (owner instanceof ServerPlayerEntity ownerPlayer) {
                    emitPlayerActionNoise(
                            world,
                            ownerPlayer,
                            itemEntity.getPos(),
                            NoiseType.DROP_ITEM,
                            0.40f,
                            6.0f
                    );
                } else {
                    emitSourceNoise(
                            world,
                            itemEntity.getPos(),
                            NoiseType.DROP_ITEM,
                            0.30f,
                            4.0f
                    );
                }
            }
        }

        KNOWN_DROPPED_ITEM_UUIDS.retainAll(stillExistingDroppedItems);
    }

    private static void emitPlayerActionNoise(
            ServerWorld world,
            ServerPlayerEntity player,
            Vec3d position,
            NoiseType noiseType,
            float strength,
            float radius
    ) {
        NoiseSystem.emitNoise(NoiseEvent.create(
                world,
                position,
                strength,
                radius,
                player,
                noiseType,
                true,
                true
        ));

        PlayerNoiseHandler.addHudNoise(player, strength);
    }

    private static void emitSourceNoise(
            ServerWorld world,
            Vec3d position,
            NoiseType noiseType,
            float strength,
            float radius
    ) {
        NoiseSystem.emitNoise(new NoiseEvent(
                world,
                position,
                strength,
                radius,
                null,
                noiseType,
                true,
                true,
                world.getTime()
        ));
    }

    private static final class PlayerActionState {
        private float lastHealth;
        private int eatingNoiseCooldownTicks;

        private PlayerActionState(float initialHealth) {
            this.lastHealth = initialHealth;
        }
    }
}