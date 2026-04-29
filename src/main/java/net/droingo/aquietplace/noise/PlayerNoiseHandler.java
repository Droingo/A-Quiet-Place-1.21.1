package net.droingo.aquietplace.noise;

import net.droingo.aquietplace.AQuietPlace;
import net.droingo.aquietplace.config.QuietPlaceConfig;
import net.droingo.aquietplace.network.NoiseLevelPayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PlayerNoiseHandler {
    private static final Map<UUID, PlayerNoiseState> PLAYER_STATES = new HashMap<>();

    private PlayerNoiseHandler() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(PlayerNoiseHandler::tickServer);
        AQuietPlace.LOGGER.info("Registered player noise handler");
    }

    private static void tickServer(MinecraftServer server) {
        Set<UUID> onlinePlayerUuids = new HashSet<>();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            onlinePlayerUuids.add(player.getUuid());
            tickPlayer(player);
        }

        PLAYER_STATES.keySet().removeIf(uuid -> !onlinePlayerUuids.contains(uuid));
    }

    private static void tickPlayer(ServerPlayerEntity player) {
        if (player.isSpectator()) {
            return;
        }

        PlayerNoiseState state = PLAYER_STATES.computeIfAbsent(
                player.getUuid(),
                uuid -> new PlayerNoiseState(player.getPos())
        );

        boolean onGround = player.isOnGround();
        Vec3d currentPosition = player.getPos();
        Vec3d previousPosition = state.lastPosition;

        double horizontalDistanceThisTick = getHorizontalDistance(previousPosition, currentPosition);

        handleJumpNoise(player, state, onGround);
        handleLandingNoise(player, state, onGround);
        handleMovementNoise(player, state, onGround, horizontalDistanceThisTick);

        sendNoiseHudUpdate(player, state);
        tickHudNoiseDecay(state);

        state.wasOnGround = onGround;
        state.lastPosition = currentPosition;

        if (!onGround) {
            state.airTicks++;
        } else {
            state.airTicks = 0;
        }

        if (state.movementNoiseCooldownTicks > 0) {
            state.movementNoiseCooldownTicks--;
        }

        if (state.hudPacketCooldownTicks > 0) {
            state.hudPacketCooldownTicks--;
        }
    }

    private static void tickHudNoiseDecay(PlayerNoiseState state) {
        QuietPlaceConfig.Hud hudConfig = QuietPlaceConfig.get().hud;

        if (state.noiseHoldTicks > 0) {
            state.noiseHoldTicks--;
            return;
        }

        state.currentNoiseLevel = Math.max(0.0f, state.currentNoiseLevel - hudConfig.noiseDecayPerTick);
    }

    private static double getHorizontalDistance(Vec3d previousPosition, Vec3d currentPosition) {
        double deltaX = currentPosition.x - previousPosition.x;
        double deltaZ = currentPosition.z - previousPosition.z;

        return Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
    }

    private static void handleMovementNoise(
            ServerPlayerEntity player,
            PlayerNoiseState state,
            boolean onGround,
            double horizontalDistanceThisTick
    ) {
        QuietPlaceConfig.PlayerNoise config = QuietPlaceConfig.get().playerNoise;

        if (!onGround) {
            return;
        }

        if (horizontalDistanceThisTick < config.minHorizontalDistancePerTick) {
            return;
        }

        if (state.movementNoiseCooldownTicks > 0) {
            return;
        }

        if (player.isSneaking()) {
            emitPlayerNoise(
                    player,
                    NoiseType.PLAYER_SNEAK,
                    config.sneakStrength,
                    config.sneakRadius,
                    true,
                    true
            );

            setHudNoiseLevel(state, config.sneakHudLevel);
            state.movementNoiseCooldownTicks = config.sneakIntervalTicks;
            return;
        }

        if (player.isSprinting()) {
            emitPlayerNoise(
                    player,
                    NoiseType.PLAYER_SPRINT,
                    config.sprintStrength,
                    config.sprintRadius,
                    true,
                    true
            );

            setHudNoiseLevel(state, config.sprintHudLevel);
            state.movementNoiseCooldownTicks = config.sprintIntervalTicks;
            return;
        }

        emitPlayerNoise(
                player,
                NoiseType.PLAYER_WALK,
                config.walkStrength,
                config.walkRadius,
                true,
                true
        );

        setHudNoiseLevel(state, config.walkHudLevel);
        state.movementNoiseCooldownTicks = config.walkIntervalTicks;
    }

    private static void handleJumpNoise(
            ServerPlayerEntity player,
            PlayerNoiseState state,
            boolean onGround
    ) {
        QuietPlaceConfig.PlayerNoise config = QuietPlaceConfig.get().playerNoise;

        boolean justLeftGround = state.wasOnGround && !onGround;

        if (!justLeftGround) {
            return;
        }

        if (player.getVelocity().y < 0.08) {
            return;
        }

        emitPlayerNoise(
                player,
                NoiseType.PLAYER_JUMP,
                config.jumpStrength,
                config.jumpRadius,
                true,
                true
        );

        setHudNoiseLevel(state, config.jumpHudLevel);
    }

    private static void handleLandingNoise(
            ServerPlayerEntity player,
            PlayerNoiseState state,
            boolean onGround
    ) {
        QuietPlaceConfig.PlayerNoise config = QuietPlaceConfig.get().playerNoise;

        boolean justLanded = !state.wasOnGround && onGround;

        if (!justLanded) {
            return;
        }

        if (state.airTicks < config.minimumLandingAirTicks) {
            return;
        }

        float radius = Math.min(
                config.landingMaxRadius,
                config.landingBaseRadius + state.airTicks * config.landingRadiusPerAirTick
        );

        float strength = Math.min(
                config.landingMaxStrength,
                config.landingBaseStrength + state.airTicks * config.landingStrengthPerAirTick
        );

        emitPlayerNoise(
                player,
                NoiseType.PLAYER_LAND,
                strength,
                radius,
                true,
                true
        );

        setHudNoiseLevel(state, strength);
    }

    public static void addHudNoise(ServerPlayerEntity player, float noiseLevel) {
        PlayerNoiseState state = PLAYER_STATES.computeIfAbsent(
                player.getUuid(),
                uuid -> new PlayerNoiseState(player.getPos())
        );

        setHudNoiseLevel(state, noiseLevel);
    }

    private static void setHudNoiseLevel(PlayerNoiseState state, float noiseLevel) {
        QuietPlaceConfig.Hud hudConfig = QuietPlaceConfig.get().hud;

        state.currentNoiseLevel = Math.max(state.currentNoiseLevel, noiseLevel);
        state.noiseHoldTicks = hudConfig.noiseHoldTicks;
    }

    private static void emitPlayerNoise(
            ServerPlayerEntity player,
            NoiseType noiseType,
            float strength,
            float radius,
            boolean visibleOnHud,
            boolean attractsDeathAngels
    ) {
        NoiseSystem.emitNoise(NoiseEvent.create(
                player.getServerWorld(),
                player.getPos(),
                strength,
                radius,
                player,
                noiseType,
                visibleOnHud,
                attractsDeathAngels
        ));
    }

    private static void sendNoiseHudUpdate(ServerPlayerEntity player, PlayerNoiseState state) {
        QuietPlaceConfig.Hud hudConfig = QuietPlaceConfig.get().hud;

        if (state.hudPacketCooldownTicks > 0) {
            return;
        }

        state.hudPacketCooldownTicks = hudConfig.packetIntervalTicks;

        if (!ServerPlayNetworking.canSend(player, NoiseLevelPayload.ID)) {
            return;
        }

        ServerPlayNetworking.send(player, new NoiseLevelPayload(clamp01(state.currentNoiseLevel)));
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static final class PlayerNoiseState {
        private boolean wasOnGround = true;
        private int airTicks;
        private int movementNoiseCooldownTicks;
        private int hudPacketCooldownTicks;
        private int noiseHoldTicks;
        private float currentNoiseLevel;
        private Vec3d lastPosition;

        private PlayerNoiseState(Vec3d initialPosition) {
            this.lastPosition = initialPosition;
        }
    }
}