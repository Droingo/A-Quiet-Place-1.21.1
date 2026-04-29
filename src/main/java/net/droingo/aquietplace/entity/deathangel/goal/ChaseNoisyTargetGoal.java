package net.droingo.aquietplace.entity.deathangel.goal;

import net.droingo.aquietplace.entity.deathangel.DeathAngelEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

public class ChaseNoisyTargetGoal extends Goal {
    private static final int NORMAL_REPATH_INTERVAL_TICKS = 12;
    private static final int CLOSE_REPATH_INTERVAL_TICKS = 6;

    private static final double MIN_TARGET_MOVE_FOR_REPATH = 1.75;
    private static final double CLOSE_RANGE = 6.0;
    private static final double VERY_CLOSE_RANGE = 3.5;
    private static final double MAX_CHASE_RANGE = 72.0;

    private final DeathAngelEntity deathAngel;
    private final double movementSpeed;

    private Entity targetEntity;
    private int repathCooldownTicks;
    private Vec3d lastPathTargetPosition;

    public ChaseNoisyTargetGoal(DeathAngelEntity deathAngel, double movementSpeed) {
        this.deathAngel = deathAngel;
        this.movementSpeed = movementSpeed;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (this.deathAngel.isPlayingHearReaction()) {
            return false;
        }

        Entity rememberedTarget = this.deathAngel.getNoisyTargetEntity();

        if (!isValidTarget(rememberedTarget)) {
            return false;
        }

        this.targetEntity = rememberedTarget;
        return true;
    }

    @Override
    public boolean shouldContinue() {
        if (this.deathAngel.isPlayingHearReaction()) {
            return false;
        }

        return this.deathAngel.hasNoisyTargetMemory() && isValidTarget(this.targetEntity);
    }

    @Override
    public void start() {
        this.repathCooldownTicks = 0;
        this.lastPathTargetPosition = null;
    }

    @Override
    public void stop() {
        this.targetEntity = null;
        this.repathCooldownTicks = 0;
        this.lastPathTargetPosition = null;
        this.deathAngel.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.targetEntity == null) {
            return;
        }
        this.deathAngel.rememberLastKnownSearchPosition(this.targetEntity.getPos(), 20 * 8);

        this.deathAngel.getLookControl().lookAt(this.targetEntity, 30.0f, 30.0f);

        double distanceSquared = this.deathAngel.squaredDistanceTo(this.targetEntity);
        double closeRangeSquared = CLOSE_RANGE * CLOSE_RANGE;
        double veryCloseRangeSquared = VERY_CLOSE_RANGE * VERY_CLOSE_RANGE;

        /*
         * Very close:
         * Let the attack goal take over instead of constantly recalculating paths.
         */
        if (distanceSquared <= veryCloseRangeSquared) {
            return;
        }

        this.repathCooldownTicks--;

        if (this.shouldRepath(distanceSquared)) {
            this.startPathToTarget(distanceSquared <= closeRangeSquared);
        }
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    private boolean shouldRepath(double distanceSquared) {
        if (this.targetEntity == null) {
            return false;
        }

        if (this.lastPathTargetPosition == null) {
            return true;
        }

        Vec3d currentTargetPosition = this.targetEntity.getPos();
        double targetMovedSquared = currentTargetPosition.squaredDistanceTo(this.lastPathTargetPosition);

        if (targetMovedSquared >= MIN_TARGET_MOVE_FOR_REPATH * MIN_TARGET_MOVE_FOR_REPATH) {
            return true;
        }

        if (this.deathAngel.getNavigation().isIdle()) {
            return true;
        }

        return this.repathCooldownTicks <= 0;
    }

    private void startPathToTarget(boolean closeToTarget) {
        if (this.targetEntity == null) {
            return;
        }

        this.lastPathTargetPosition = this.targetEntity.getPos();

        this.deathAngel.getNavigation().startMovingTo(this.targetEntity, this.movementSpeed);

        this.repathCooldownTicks = closeToTarget
                ? CLOSE_REPATH_INTERVAL_TICKS
                : NORMAL_REPATH_INTERVAL_TICKS;
    }

    private boolean isValidTarget(Entity entity) {
        if (!this.deathAngel.canHuntEntity(entity)) {
            return false;
        }

        return this.deathAngel.squaredDistanceTo(entity) <= MAX_CHASE_RANGE * MAX_CHASE_RANGE;
    }
}