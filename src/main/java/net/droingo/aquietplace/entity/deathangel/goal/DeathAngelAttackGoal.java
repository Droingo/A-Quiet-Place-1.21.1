package net.droingo.aquietplace.entity.deathangel.goal;

import net.droingo.aquietplace.entity.deathangel.DeathAngelEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;

import java.util.EnumSet;

public class DeathAngelAttackGoal extends Goal {
    private static final double ATTACK_RANGE = 3.2;
    private static final double ATTACK_CONTINUE_RANGE = 5.0;
    private static final int ATTACK_WINDUP_TICKS = 7;
    private static final int RUN_ATTACK_ANIMATION_TICKS = 18;
    private static final int ATTACK_COOLDOWN_TICKS = 18;
    private static final int POST_ATTACK_MEMORY_TICKS = 20 * 5;
    private static final double RUN_ATTACK_SPEED = 1.75;
    private static final int POST_ATTACK_SUPPRESS_HEAR_TICKS = 20 * 5;

    private final DeathAngelEntity deathAngel;

    private Entity targetEntity;
    private int windupTicks;
    private int repathCooldownTicks;
    private boolean hasDealtDamage;

    public DeathAngelAttackGoal(DeathAngelEntity deathAngel) {
        this.deathAngel = deathAngel;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (this.deathAngel.isPlayingHearReaction()) {
            return false;
        }

        if (!this.deathAngel.hasNoisyTargetMemory()) {
            return false;
        }

        if (!this.deathAngel.canAttackNow()) {
            return false;
        }

        Entity rememberedTarget = this.deathAngel.getNoisyTargetEntity();

        if (!isValidTarget(rememberedTarget)) {
            return false;
        }

        if (this.deathAngel.squaredDistanceTo(rememberedTarget) > ATTACK_RANGE * ATTACK_RANGE) {
            return false;
        }

        this.targetEntity = rememberedTarget;
        return true;
    }

    @Override
    public boolean shouldContinue() {
        if (this.targetEntity == null) {
            return false;
        }

        if (!isValidTarget(this.targetEntity)) {
            return false;
        }

        if (this.windupTicks <= 0 && this.hasDealtDamage) {
            return false;
        }

        return this.deathAngel.squaredDistanceTo(this.targetEntity) <= ATTACK_CONTINUE_RANGE * ATTACK_CONTINUE_RANGE;
    }

    @Override
    public void start() {
        this.windupTicks = ATTACK_WINDUP_TICKS;
        this.repathCooldownTicks = 0;
        this.hasDealtDamage = false;

        refreshTargetMemory();

        this.deathAngel.startRunAttackAnimation(RUN_ATTACK_ANIMATION_TICKS);
    }

    @Override
    public void stop() {
        refreshTargetMemory();

        this.targetEntity = null;
        this.windupTicks = 0;
        this.repathCooldownTicks = 0;
        this.hasDealtDamage = false;
        this.deathAngel.setAttackCooldown(ATTACK_COOLDOWN_TICKS);
    }

    @Override
    public void tick() {
        if (this.targetEntity == null) {
            return;
        }

        refreshTargetMemory();

        this.deathAngel.getLookControl().lookAt(this.targetEntity, 40.0f, 40.0f);

        this.repathCooldownTicks--;

        if (this.repathCooldownTicks <= 0) {
            this.deathAngel.getNavigation().startMovingTo(this.targetEntity, RUN_ATTACK_SPEED);
            this.repathCooldownTicks = 4;
        }

        if (this.windupTicks > 0) {
            this.windupTicks--;
            return;
        }

        if (!this.hasDealtDamage) {
            if (this.deathAngel.squaredDistanceTo(this.targetEntity) <= ATTACK_RANGE * ATTACK_RANGE) {
                this.deathAngel.tryAttack(this.targetEntity);
            }

            this.hasDealtDamage = true;
            refreshTargetMemory();
        }
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    private void refreshTargetMemory() {
        if (this.targetEntity == null) {
            return;
        }

        this.deathAngel.rememberNoisyTarget(this.targetEntity.getUuid(), POST_ATTACK_MEMORY_TICKS);
        this.deathAngel.suppressHearReaction(POST_ATTACK_SUPPRESS_HEAR_TICKS);
    }

    private boolean isValidTarget(Entity entity) {
        if (!(entity instanceof PlayerEntity player)) {
            return false;
        }

        return player.isAlive() && !player.isSpectator();
    }
}