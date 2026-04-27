package net.droingo.aquietplace.entity.deathangel.goal;

import net.droingo.aquietplace.entity.deathangel.DeathAngelEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;

import java.util.EnumSet;

public class ChaseNoisyTargetGoal extends Goal {
    private final DeathAngelEntity deathAngel;
    private final double movementSpeed;

    private Entity targetEntity;
    private int repathCooldownTicks;

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
    }

    @Override
    public void stop() {
        this.targetEntity = null;
        this.repathCooldownTicks = 0;
        this.deathAngel.clearClimbTargetPosition();
        this.deathAngel.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.targetEntity == null) {
            return;
        }
        this.deathAngel.setClimbTargetPosition(this.targetEntity.getPos());
        this.deathAngel.getLookControl().lookAt(this.targetEntity, 30.0f, 30.0f);

        this.repathCooldownTicks--;

        if (this.repathCooldownTicks <= 0) {
            this.deathAngel.getNavigation().startMovingTo(this.targetEntity, this.movementSpeed);
            this.repathCooldownTicks = 6;
        }
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    private boolean isValidTarget(Entity entity) {
        if (!(entity instanceof PlayerEntity player)) {
            return false;
        }

        if (!player.isAlive()) {
            return false;
        }

        // Ignore spectators, but allow Creative players for development testing.
        if (player.isSpectator()) {
            return false;
        }

        return this.deathAngel.squaredDistanceTo(player) <= 64.0 * 64.0;
    }
}