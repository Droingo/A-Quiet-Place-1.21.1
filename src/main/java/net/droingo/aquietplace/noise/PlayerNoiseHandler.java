package net.droingo.aquietplace.noise;

import net.droingo.aquietplace.AQuietPlace;
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

    private static final int WALK_NOISE_INTERVAL_TICKS = 14;
    private static final int SPRINT_NOISE_INTERVAL_TICKS = 8;
    private static final int SNEAK_NOISE_INTERVAL_TICKS = 24;
    private static final int HUD_PACKET_INTERVAL_TICKS = 2;

    private static final double MIN_HORIZONTAL_DISTANCE_PER_TICK = 0.015;
    private static final float NOISE_LEVEL_DECAY_PER_TICK = 0.035f;

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

        state.currentNoiseLevel = Math.max(0.0f, state.currentNoiseLevel - NOISE_LEVEL_DECAY_PER_TICK);

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
        if (!onGround) {
            return;
        }

        if (horizontalDistanceThisTick < MIN_HORIZONTAL_DISTANCE_PER_TICK) {
            return;
        }

        if (state.movementNoiseCooldownTicks > 0) {
            return;
        }

        if (player.isSneaking()) {
            emitPlayerNoise(
                    player,
                    NoiseType.PLAYER_SNEAK,
                    0.25f,
                    1.5f,
                    true,
                    true
            );

            state.currentNoiseLevel = Math.max(state.currentNoiseLevel, 0.15f);
            state.movementNoiseCooldownTicks = SNEAK_NOISE_INTERVAL_TICKS;
            return;
        }

        if (player.isSprinting()) {
            emitPlayerNoise(
                    player,
                    NoiseType.PLAYER_SPRINT,
                    1.0f,
                    18.0f,
                    true,
                    true
            );

            state.currentNoiseLevel = Math.max(state.currentNoiseLevel, 1.0f);
            state.movementNoiseCooldownTicks = SPRINT_NOISE_INTERVAL_TICKS;
            return;
        }

        emitPlayerNoise(
                player,
                NoiseType.PLAYER_WALK,
                0.45f,
                5.0f,
                true,
                true
        );

        state.currentNoiseLevel = Math.max(state.currentNoiseLevel, 0.45f);
        state.movementNoiseCooldownTicks = WALK_NOISE_INTERVAL_TICKS;
    }

    private static void handleJumpNoise(
            ServerPlayerEntity player,
            PlayerNoiseState state,
            boolean onGround
    ) {
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
                0.65f,
                8.0f,
                true,
                true
        );

        state.currentNoiseLevel = Math.max(state.currentNoiseLevel, 0.65f);
    }

    private static void handleLandingNoise(
            ServerPlayerEntity player,
            PlayerNoiseState state,
            boolean onGround
    ) {
        boolean justLanded = !state.wasOnGround && onGround;

        if (!justLanded) {
            return;
        }

        if (state.airTicks < 6) {
            return;
        }

        float radius = Math.min(30.0f, 4.0f + state.airTicks * 0.75f);
        float strength = Math.min(1.0f, 0.35f + state.airTicks * 0.035f);

        emitPlayerNoise(
                player,
                NoiseType.PLAYER_LAND,
                strength,
                radius,
                true,
                true
        );

        state.currentNoiseLevel = Math.max(state.currentNoiseLevel, strength);
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
        if (state.hudPacketCooldownTicks > 0) {
            return;
        }

        state.hudPacketCooldownTicks = HUD_PACKET_INTERVAL_TICKS;

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
        private float currentNoiseLevel;
        private Vec3d lastPosition;

        private PlayerNoiseState(Vec3d initialPosition) {
            this.lastPosition = initialPosition;
        }
    }
}