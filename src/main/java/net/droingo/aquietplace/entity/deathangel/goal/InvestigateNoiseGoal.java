package net.droingo.aquietplace.entity.deathangel.goal;

import net.droingo.aquietplace.entity.deathangel.DeathAngelEntity;
import net.droingo.aquietplace.noise.NoiseEvent;
import net.droingo.aquietplace.noise.NoiseSystem;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public class InvestigateNoiseGoal extends Goal {
    private final DeathAngelEntity deathAngel;
    private final double movementSpeed;

    private Vec3d targetPosition;
    private long targetNoiseGameTime = -1L;
    private UUID targetSourceUuid;
    private float targetStrength;
    private float targetRadius;

    private long lastHandledNoiseGameTime = -1L;

    private int investigateTicks;
    private int repathCooldownTicks;
    private int listenDelayTicks;
    private int newNoiseCheckCooldownTicks;

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

        if (this.deathAngel.hasNoisyTargetMemory()) {
            return false;
        }

        if (!(this.deathAngel.getWorld() instanceof ServerWorld serverWorld)) {
            return false;
        }

        NoiseEvent bestNoise = this.findBestNoise(serverWorld);
        if (bestNoise == null) {
            return false;
        }

        this.setTargetNoise(bestNoise);
        return true;
    }

    @Override
    public boolean shouldContinue() {
        if (this.deathAngel.hasNoisyTargetMemory() && !this.deathAngel.isPlayingHearReaction()) {
            return false;
        }

        if (this.targetPosition == null) {
            return false;
        }

        if (this.investigateTicks <= 0) {
            return false;
        }

        if (this.listenDelayTicks > 0 || this.deathAngel.isPlayingHearReaction()) {
            return true;
        }

        double distanceSquared = this.deathAngel.getPos().squaredDistanceTo(this.targetPosition);

        return distanceSquared > 2.25 || this.isHeavyInvestigation();
    }

    @Override
    public void start() {
        this.resetInvestigationTimers();

        this.lastHandledNoiseGameTime = this.targetNoiseGameTime;

        boolean triggersHunt = this.shouldTriggerHunt();

        /*
         * Decide before rememberNoisyTarget(...) refreshes recent hunt memory.
         *
         * Dangerous player noise should play the hearing animation when this is not
         * already the same recent hunted player.
         */
        boolean shouldPlayHear = this.shouldPlayHearBeforeHunt();
        if (triggersHunt) {
            this.deathAngel.rememberNoisyTarget(this.targetSourceUuid, this.getHuntMemoryTicks());
        }

        this.deathAngel.getNavigation().stop();

        if (shouldPlayHear) {
            this.deathAngel.playHearReaction(this.targetPosition);
            this.listenDelayTicks = Math.max(this.listenDelayTicks, this.deathAngel.getHearReactionTicksRemaining());
        } else {
            this.listenDelayTicks = 0;
        }
    }

    @Override
    public void stop() {
        this.targetPosition = null;
        this.targetNoiseGameTime = -1L;
        this.targetSourceUuid = null;
        this.targetStrength = 0.0f;
        this.targetRadius = 0.0f;

        this.investigateTicks = 0;
        this.repathCooldownTicks = 0;
        this.listenDelayTicks = 0;
        this.newNoiseCheckCooldownTicks = 0;

        this.deathAngel.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.targetPosition == null) {
            return;
        }

        this.investigateTicks--;

        this.checkForNewerNoise();

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

        if (this.deathAngel.isPlayingHearReaction()) {
            this.deathAngel.getNavigation().stop();
            return;
        }

        if (this.deathAngel.hasNoisyTargetMemory()) {
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

    private void checkForNewerNoise() {
        if (this.deathAngel.getWorld().isClient()) {
            return;
        }

        if (!(this.deathAngel.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        this.newNoiseCheckCooldownTicks--;

        if (this.newNoiseCheckCooldownTicks > 0) {
            return;
        }

        this.newNoiseCheckCooldownTicks = 4;

        NoiseEvent newerNoise = this.findBestNoise(serverWorld);

        if (newerNoise == null) {
            return;
        }

        this.setTargetNoise(newerNoise);
        this.lastHandledNoiseGameTime = newerNoise.gameTime();

        /*
         * Important:
         * If a newer player-made loud sound is heard while listening,
         * refresh hunt memory but do NOT restart HearLeft/HearRight.
         *
         * This prevents the Death Angel from getting stuck in endless listening
         * while the player keeps sprinting/jumping.
         */
        if (this.shouldTriggerHunt()) {
            /*
             * Decide before rememberNoisyTarget(...) updates hunt memory.
             */
            boolean shouldPlayHear = this.shouldPlayHearBeforeHunt();

            this.deathAngel.rememberNoisyTarget(this.targetSourceUuid, this.getHuntMemoryTicks());
            this.investigateTicks = Math.max(this.investigateTicks, 20 * 4);

            if (shouldPlayHear && !this.deathAngel.isPlayingHearReaction()) {
                this.deathAngel.getNavigation().stop();
                this.deathAngel.playHearReaction(this.targetPosition);
                this.listenDelayTicks = Math.max(this.listenDelayTicks, this.deathAngel.getHearReactionTicksRemaining());
            } else {
                this.listenDelayTicks = 0;
            }

            return;
        }
        /*
         * For newer source-based sounds, update the investigation target.
         * Only play a fresh hear reaction if it is not already listening.
         */
        this.resetInvestigationTimers();

        if (!this.deathAngel.isPlayingHearReaction()) {
            this.deathAngel.getNavigation().stop();
            this.deathAngel.playHearReaction(this.targetPosition);
            this.listenDelayTicks = Math.max(this.listenDelayTicks, this.deathAngel.getHearReactionTicksRemaining());
        } else {
            this.listenDelayTicks = Math.max(this.listenDelayTicks, this.deathAngel.getHearReactionTicksRemaining());
        }
    }

    private void setTargetNoise(NoiseEvent noiseEvent) {
        this.targetPosition = noiseEvent.position();
        this.targetNoiseGameTime = noiseEvent.gameTime();
        this.targetSourceUuid = noiseEvent.sourceEntityUuid();
        this.targetStrength = noiseEvent.strength();
        this.targetRadius = noiseEvent.radius();
    }

    private void resetInvestigationTimers() {
        this.investigateTicks = this.isHeavyInvestigation() ? 20 * 12 : 20 * 8;
        this.repathCooldownTicks = 0;
        this.listenDelayTicks = this.isHeavyInvestigation() ? 30 : 20;
        this.newNoiseCheckCooldownTicks = 0;
    }

    private void moveToTarget() {
        if (this.targetPosition == null) {
            return;
        }

        this.deathAngel.getNavigation().startMovingTo(
                this.targetPosition.x,
                this.targetPosition.y,
                this.targetPosition.z,
                this.getInvestigationSpeed()
        );
    }

    private boolean shouldPlayHearBeforeHunt() {
        /*
         * Very loud noises are immediate threats.
         * Example: big falls, huge impacts, future traps/decoys.
         * The Death Angel should snap into action instead of giving the player
         * several seconds while HearLeft/HearRight plays.
         */
        if (this.isVeryLoudNoise()) {
            return false;
        }

        if (!this.shouldTriggerHunt()) {
            return true;
        }

        /*
         * If the Death Angel is already actively pressuring players,
         * any new loud player noise should redirect the hunt immediately.
         */
        if (this.deathAngel.isActivelyHuntingOrSearching()) {
            return false;
        }

        /*
         * Calm/idle first dangerous sound:
         * play HearLeft/HearRight before chasing, unless it crossed the very-loud threshold above.
         */
        if (this.isDangerousPlayerNoise()) {
            return true;
        }

        return this.deathAngel.shouldPlayHearReactionForHunt(this.targetSourceUuid);
    }

    private boolean isDangerousPlayerNoise() {
        return this.targetSourceUuid != null && (this.targetStrength >= 0.95f || this.targetRadius >= 18.0f);
    }

    private boolean isVeryLoudNoise() {
        return this.targetStrength >= 1.0f || this.targetRadius >= 22.0f;
    }

    private boolean shouldTriggerHunt() {
        if (this.targetSourceUuid == null) {
            return false;
        }

        if (!this.deathAngel.canHuntEntity(this.targetSourceUuid)) {
            return false;
        }

        return this.targetStrength >= 0.8f || this.targetRadius >= 12.0f;
    }

    private boolean isHeavyInvestigation() {
        return this.targetSourceUuid == null && (this.targetStrength >= 0.65f || this.targetRadius >= 10.0f);
    }

    private double getInvestigationSpeed() {
        if (this.isHeavyInvestigation()) {
            return this.movementSpeed * 1.15;
        }

        return this.movementSpeed;
    }

    private int getHuntMemoryTicks() {
        if (this.targetRadius >= 24.0f) {
            return 20 * 6;
        }

        if (this.targetRadius >= 18.0f) {
            return 20 * 4;
        }

        return 20 * 3;
    }

    private NoiseEvent findBestNoise(ServerWorld serverWorld) {
        List<NoiseEvent> hearableNoises = NoiseSystem.getHearableNoises(
                serverWorld,
                this.deathAngel.getPos()
        );

        NoiseEvent bestNoise = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (NoiseEvent noiseEvent : hearableNoises) {
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