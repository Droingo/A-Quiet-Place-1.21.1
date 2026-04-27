package net.droingo.aquietplace.entity.deathangel.goal;

import net.droingo.aquietplace.entity.deathangel.DeathAngelEntity;
import net.droingo.aquietplace.noise.NoiseEvent;
import net.droingo.aquietplace.noise.NoiseSystem;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;
import java.util.List;

public class InvestigateNoiseGoal extends Goal {
    private final DeathAngelEntity deathAngel;
    private final double movementSpeed;

    private Vec3d targetPosition;
    private long targetNoiseGameTime = -1L;

    private long lastHandledNoiseGameTime = -1L;

    private int investigateTicks;
    private int repathCooldownTicks;
    private int listenDelayTicks;

    public InvestigateNoiseGoal(DeathAngelEntity deathAngel, double movementSpeed) {
        this.deathAngel = deathAngel;
        this.movementSpeed = movementSpeed;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (this.deathAngel.getWorld().isClient()) {
            return false;
        }

        if (!(this.deathAngel.getWorld() instanceof ServerWorld serverWorld)) {
            return false;
        }

        NoiseEvent bestNoise = this.findBestNoise(serverWorld);
        if (bestNoise == null) {
            return false;
        }

        this.targetPosition = bestNoise.position();
        this.targetNoiseGameTime = bestNoise.gameTime();

        return true;
    }

    @Override
    public boolean shouldContinue() {
        if (this.targetPosition == null) {
            return false;
        }

        if (this.investigateTicks <= 0) {
            return false;
        }

        if (this.listenDelayTicks > 0) {
            return true;
        }

        double distanceSquared = this.deathAngel.getPos().squaredDistanceTo(this.targetPosition);

        // Stop once close enough to the noise source.
        return distanceSquared > 2.25;
    }

    @Override
    public void start() {
        this.investigateTicks = 20 * 8;
        this.repathCooldownTicks = 0;
        this.listenDelayTicks = 20;

        // Mark this noise as handled immediately so this same goal does not restart on it later.
        this.lastHandledNoiseGameTime = this.targetNoiseGameTime;

        this.deathAngel.getNavigation().stop();
        this.deathAngel.playHearReaction(this.targetPosition);
    }

    @Override
    public void stop() {
        this.targetPosition = null;
        this.targetNoiseGameTime = -1L;
        this.investigateTicks = 0;
        this.repathCooldownTicks = 0;
        this.listenDelayTicks = 0;
        this.deathAngel.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.targetPosition == null) {
            return;
        }

        this.investigateTicks--;

        this.deathAngel.getLookControl().lookAt(
                this.targetPosition.x,
                this.targetPosition.y,
                this.targetPosition.z
        );

        if (this.listenDelayTicks > 0) {
            this.listenDelayTicks--;
            this.deathAngel.getNavigation().stop();
            return;
        }

        this.repathCooldownTicks--;

        if (this.repathCooldownTicks <= 0) {
            this.moveToTarget();
            this.repathCooldownTicks = 10;
        }
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    private void moveToTarget() {
        if (this.targetPosition == null) {
            return;
        }

        this.deathAngel.getNavigation().startMovingTo(
                this.targetPosition.x,
                this.targetPosition.y,
                this.targetPosition.z,
                this.movementSpeed
        );
    }

    private NoiseEvent findBestNoise(ServerWorld serverWorld) {
        List<NoiseEvent> hearableNoises = NoiseSystem.getHearableNoises(
                serverWorld,
                this.deathAngel.getPos()
        );

        NoiseEvent bestNoise = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (NoiseEvent noiseEvent : hearableNoises) {
            // Do not react to the same already-handled noise again.
            if (noiseEvent.gameTime() <= this.lastHandledNoiseGameTime) {
                continue;
            }

            double distance = Math.sqrt(noiseEvent.position().squaredDistanceTo(this.deathAngel.getPos()));

            double score =
                    noiseEvent.strength() * 2.0
                            + noiseEvent.radius()
                            - distance * 0.35;

            if (score > bestScore) {
                bestScore = score;
                bestNoise = noiseEvent;
            }
        }

        return bestNoise;
    }
}