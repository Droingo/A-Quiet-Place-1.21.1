package net.droingo.aquietplace.noise;

import net.droingo.aquietplace.AQuietPlace;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class NoiseSystem {
    private static final List<NoiseEvent> RECENT_NOISES = new ArrayList<>();

    private static final long MAX_NOISE_AGE_TICKS = 20L * 10L;
    private static boolean debugLoggingEnabled = true;

    private NoiseSystem() {
    }

    public static void initialize() {
        AQuietPlace.LOGGER.info("Noise system initialized");
    }

    public static void emitNoise(NoiseEvent noiseEvent) {
        RECENT_NOISES.add(noiseEvent);

        if (debugLoggingEnabled) {
            AQuietPlace.LOGGER.info(
                    "Noise emitted: type={}, strength={}, radius={}, pos={}, source={}",
                    noiseEvent.type(),
                    noiseEvent.strength(),
                    noiseEvent.radius(),
                    noiseEvent.position(),
                    noiseEvent.getSourceEntityUuid().map(Object::toString).orElse("none")
            );
        }
    }

    public static List<NoiseEvent> getRecentNoises(ServerWorld world, Vec3d listenerPosition, double maxDistance) {
        cleanupExpiredNoises(world);

        double maxDistanceSquared = maxDistance * maxDistance;
        List<NoiseEvent> result = new ArrayList<>();

        for (NoiseEvent noiseEvent : RECENT_NOISES) {
            if (noiseEvent.world() != world) {
                continue;
            }

            if (noiseEvent.position().squaredDistanceTo(listenerPosition) <= maxDistanceSquared) {
                result.add(noiseEvent);
            }
        }

        return result;
    }

    public static List<NoiseEvent> getHearableNoises(ServerWorld world, Vec3d listenerPosition) {
        cleanupExpiredNoises(world);

        List<NoiseEvent> result = new ArrayList<>();

        for (NoiseEvent noiseEvent : RECENT_NOISES) {
            if (noiseEvent.world() != world) {
                continue;
            }

            if (!noiseEvent.attractsDeathAngels()) {
                continue;
            }

            if (noiseEvent.canBeHeardFrom(listenerPosition)) {
                result.add(noiseEvent);
            }
        }

        return result;
    }

    public static void cleanupExpiredNoises(ServerWorld world) {
        long currentTime = world.getTime();

        Iterator<NoiseEvent> iterator = RECENT_NOISES.iterator();
        while (iterator.hasNext()) {
            NoiseEvent noiseEvent = iterator.next();

            if (noiseEvent.world() == world && noiseEvent.isExpired(currentTime, MAX_NOISE_AGE_TICKS)) {
                iterator.remove();
            }
        }
    }

    public static void setDebugLoggingEnabled(boolean enabled) {
        debugLoggingEnabled = enabled;
    }

    public static boolean isDebugLoggingEnabled() {
        return debugLoggingEnabled;
    }
}