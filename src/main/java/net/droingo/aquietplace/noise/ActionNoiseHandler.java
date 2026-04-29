package net.droingo.aquietplace.noise;

import net.droingo.aquietplace.AQuietPlace;
import net.droingo.aquietplace.config.QuietPlaceConfig;
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

            QuietPlaceConfig.ActionNoise config = QuietPlaceConfig.get().actionNoise;

            emitPlayerActionNoise(
                    serverWorld,
                    serverPlayer,
                    player.getPos(),
                    NoiseType.ATTACK,
                    config.attackStrength,
                    config.attackRadius
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
        QuietPlaceConfig.ActionNoise config = QuietPlaceConfig.get().actionNoise;

        float currentHealth = player.getHealth();

        if (currentHealth >= state.lastHealth) {
            return;
        }

        float lostHealth = state.lastHealth - currentHealth;

        float strength = Math.min(
                config.damageMaxStrength,
                config.damageBaseStrength + lostHealth * config.damageStrengthPerHealthLost
        );

        float radius = Math.min(
                config.damageMaxRadius,
                config.damageBaseRadius + lostHealth * config.damageRadiusPerHealthLost
        );

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
        QuietPlaceConfig.ActionNoise config = QuietPlaceConfig.get().actionNoise;

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

        emitPlayerActionNoise(
                player.getServerWorld(),
                player,
                player.getPos(),
                NoiseType.EAT,
                config.eatStrength,
                config.eatRadius
        );

        state.eatingNoiseCooldownTicks = config.eatingNoiseIntervalTicks;
    }

    private static void scanDroppedItems(MinecraftServer server) {
        QuietPlaceConfig.ActionNoise config = QuietPlaceConfig.get().actionNoise;

        if (server.getTicks() % config.dropScanIntervalTicks != 0) {
            return;
        }

        Set<UUID> stillExistingDroppedItems = new HashSet<>();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.isSpectator()) {
                continue;
            }

            ServerWorld world = player.getServerWorld();
            Box scanBox = player.getBoundingBox().expand(config.droppedItemScanRadius);

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
                            config.dropItemPlayerStrength,
                            config.dropItemPlayerRadius
                    );
                } else {
                    emitSourceNoise(
                            world,
                            itemEntity.getPos(),
                            NoiseType.DROP_ITEM,
                            config.dropItemUnknownStrength,
                            config.dropItemUnknownRadius
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