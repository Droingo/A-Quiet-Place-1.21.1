package net.droingo.aquietplace.noise;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;
import java.util.UUID;

public record NoiseEvent(
        ServerWorld world,
        Vec3d position,
        float strength,
        float radius,
        UUID sourceEntityUuid,
        NoiseType type,
        boolean visibleOnHud,
        boolean attractsDeathAngels,
        long gameTime
) {
    public NoiseEvent {
        if (world == null) {
            throw new IllegalArgumentException("NoiseEvent world cannot be null");
        }

        if (position == null) {
            throw new IllegalArgumentException("NoiseEvent position cannot be null");
        }

        if (type == null) {
            type = NoiseType.UNKNOWN;
        }

        strength = Math.max(0.0f, strength);
        radius = Math.max(0.0f, radius);
    }

    public static NoiseEvent create(
            ServerWorld world,
            Vec3d position,
            float strength,
            float radius,
            Entity sourceEntity,
            NoiseType type,
            boolean visibleOnHud,
            boolean attractsDeathAngels
    ) {
        UUID sourceUuid = sourceEntity == null ? null : sourceEntity.getUuid();

        return new NoiseEvent(
                world,
                position,
                strength,
                radius,
                sourceUuid,
                type,
                visibleOnHud,
                attractsDeathAngels,
                world.getTime()
        );
    }

    public Optional<UUID> getSourceEntityUuid() {
        return Optional.ofNullable(sourceEntityUuid);
    }

    public boolean isExpired(long currentGameTime, long maxAgeTicks) {
        return currentGameTime - gameTime > maxAgeTicks;
    }

    public boolean canBeHeardFrom(Vec3d listenerPosition) {
        return position.squaredDistanceTo(listenerPosition) <= radius * radius;
    }
}