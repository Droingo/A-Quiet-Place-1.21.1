package net.droingo.aquietplace.noise;

import net.droingo.aquietplace.AQuietPlace;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class NoiseSystem {
    private static final List<NoiseEvent> RECENT_NOISES = new ArrayList<>();

    private static final long MAX_NOISE_AGE_TICKS = 20L * 10L;

    private static boolean debugLoggingEnabled = true;
    private static boolean debugParticlesEnabled = true;

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

        if (debugParticlesEnabled) {
            spawnDebugParticles(noiseEvent);
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

    private static void spawnDebugParticles(NoiseEvent noiseEvent) {
        Vec3d position = noiseEvent.position();
        ServerWorld world = noiseEvent.world();

        double x = position.x;
        double y = position.y + 0.15;
        double z = position.z;

        int centerCount = Math.max(8, Math.min(40, Math.round(noiseEvent.radius())));
        double ringRadius = Math.min(noiseEvent.radius(), 32.0f);

        world.spawnParticles(
                ParticleTypes.SONIC_BOOM,
                x,
                y + 0.5,
                z,
                1,
                0.0,
                0.0,
                0.0,
                0.0
        );

        world.spawnParticles(
                ParticleTypes.NOTE,
                x,
                y + 1.0,
                z,
                centerCount,
                0.5,
                0.5,
                0.5,
                0.05
        );

        spawnNoiseRadiusRing(world, x, y, z, ringRadius);
    }

    private static void spawnNoiseRadiusRing(ServerWorld world, double centerX, double centerY, double centerZ, double radius) {
        if (radius <= 0.0) {
            return;
        }

        int points = Math.max(24, Math.min(96, (int) Math.round(radius * 4.0)));

        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0 * i) / points;
            double x = centerX + Math.cos(angle) * radius;
            double z = centerZ + Math.sin(angle) * radius;

            world.spawnParticles(
                    ParticleTypes.SCULK_SOUL,
                    x,
                    centerY + 0.05,
                    z,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            );
        }
    }

    public static void setDebugLoggingEnabled(boolean enabled) {
        debugLoggingEnabled = enabled;
    }

    public static boolean isDebugLoggingEnabled() {
        return debugLoggingEnabled;
    }

    public static void setDebugParticlesEnabled(boolean enabled) {
        debugParticlesEnabled = enabled;
    }

    public static boolean isDebugParticlesEnabled() {
        return debugParticlesEnabled;
    }
}