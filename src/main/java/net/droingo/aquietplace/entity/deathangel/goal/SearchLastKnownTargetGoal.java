package net.droingo.aquietplace.entity.deathangel.goal;

import net.droingo.aquietplace.entity.deathangel.DeathAngelEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

public class SearchLastKnownTargetGoal extends Goal {
    private static final int REPATH_INTERVAL_TICKS = 12;
    private static final int HEAR_REACTION_COOLDOWN_TICKS = 70;

    private static final double SEARCH_REACH_DISTANCE = 2.5;
    private static final double SEARCH_RADIUS = 7.0;

    private final DeathAngelEntity deathAngel;
    private final double movementSpeed;

    private Vec3d searchCenter;
    private Vec3d currentSearchPoint;

    private int repathCooldownTicks;
    private int searchListenPauseTicks;
    private int hearReactionCooldownTicks;

    public SearchLastKnownTargetGoal(DeathAngelEntity deathAngel, double movementSpeed) {
        this.deathAngel = deathAngel;
        this.movementSpeed = movementSpeed;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (this.deathAngel.isPlayingHearReaction()) {
            return false;
        }

        if (this.deathAngel.hasNoisyTargetMemory()) {
            return false;
        }

        if (!this.deathAngel.hasLastKnownSearchPosition()) {
            return false;
        }

        this.searchCenter = this.deathAngel.getLastKnownSearchPosition();
        return this.searchCenter != null;
    }

    @Override
    public boolean shouldContinue() {
        if (this.deathAngel.hasNoisyTargetMemory()) {
            return false;
        }

        return this.deathAngel.hasLastKnownSearchPosition();
    }

    @Override
    public void start() {
        this.repathCooldownTicks = 0;
        this.searchListenPauseTicks = 0;
        this.hearReactionCooldownTicks = 0;

        this.searchCenter = this.deathAngel.getLastKnownSearchPosition();
        this.currentSearchPoint = chooseRandomSearchPoint();
    }

    @Override
    public void stop() {
        this.searchCenter = null;
        this.currentSearchPoint = null;
        this.repathCooldownTicks = 0;
        this.searchListenPauseTicks = 0;
        this.hearReactionCooldownTicks = 0;
        this.deathAngel.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.searchCenter == null) {
            this.searchCenter = this.deathAngel.getLastKnownSearchPosition();
        }

        if (this.searchCenter == null) {
            return;
        }

        /*
         * While actively searching, new player noise should resume the hunt quickly.
         * This suppresses repeated hunt-listen delays, but does not stop this search
         * goal from deliberately playing its own search-listen animation.
         */
        this.deathAngel.suppressHearReaction(10);

        if (this.hearReactionCooldownTicks > 0) {
            this.hearReactionCooldownTicks--;
        }

        if (this.searchListenPauseTicks > 0) {
            this.searchListenPauseTicks--;
            this.deathAngel.getNavigation().stop();
            return;
        }

        if (this.currentSearchPoint == null) {
            this.currentSearchPoint = chooseRandomSearchPoint();
            return;
        }

        this.deathAngel.getLookControl().lookAt(
                this.currentSearchPoint.x,
                this.currentSearchPoint.y,
                this.currentSearchPoint.z
        );

        double distanceSquared = this.deathAngel.getPos().squaredDistanceTo(this.currentSearchPoint);

        if (distanceSquared <= SEARCH_REACH_DISTANCE * SEARCH_REACH_DISTANCE) {
            onReachedSearchPoint();
            return;
        }

        this.repathCooldownTicks--;

        if (this.repathCooldownTicks <= 0) {
            this.deathAngel.getNavigation().startMovingTo(
                    this.currentSearchPoint.x,
                    this.currentSearchPoint.y,
                    this.currentSearchPoint.z,
                    this.movementSpeed
            );

            this.repathCooldownTicks = REPATH_INTERVAL_TICKS;
        }
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    private void onReachedSearchPoint() {
        this.deathAngel.getNavigation().stop();

        if (this.hearReactionCooldownTicks <= 0) {
            Vec3d listenToward = chooseRandomSearchPoint();

            if (listenToward == null) {
                listenToward = this.currentSearchPoint;
            }

            this.deathAngel.playHearReaction(listenToward);
            this.searchListenPauseTicks = Math.max(20, this.deathAngel.getHearReactionTicksRemaining());
            this.hearReactionCooldownTicks = HEAR_REACTION_COOLDOWN_TICKS;
        }

        this.currentSearchPoint = chooseRandomSearchPoint();
        this.repathCooldownTicks = 0;
    }

    private Vec3d chooseRandomSearchPoint() {
        if (this.searchCenter == null) {
            return null;
        }

        double angle = this.deathAngel.getRandom().nextDouble() * Math.PI * 2.0;
        double distance = 2.0 + this.deathAngel.getRandom().nextDouble() * SEARCH_RADIUS;

        double x = this.searchCenter.x + Math.cos(angle) * distance;
        double z = this.searchCenter.z + Math.sin(angle) * distance;

        return new Vec3d(x, this.searchCenter.y, z);
    }
}