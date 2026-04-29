package net.droingo.aquietplace.entity.deathangel;

import net.droingo.aquietplace.entity.deathangel.goal.ChaseNoisyTargetGoal;
import net.droingo.aquietplace.entity.deathangel.goal.DeathAngelAttackGoal;
import net.droingo.aquietplace.entity.deathangel.goal.InvestigateNoiseGoal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import com.nyfaria.awcapi.entity.ClimberComponent;
import com.nyfaria.awcapi.entity.IAdvancedClimber;
import com.nyfaria.awcapi.ClimberHelper;
import net.minecraft.entity.MovementType;
import com.nyfaria.awcapi.entity.movement.ClimberPathNavigator;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.droingo.aquietplace.entity.deathangel.goal.SearchLastKnownTargetGoal;
import net.droingo.aquietplace.config.QuietPlaceConfig;


import java.util.UUID;

public class DeathAngelEntity extends HostileEntity implements GeoEntity, IAdvancedClimber {
    private static final TrackedData<Boolean> HEAR_REACTION_ACTIVE = DataTracker.registerData(
            DeathAngelEntity.class,
            TrackedDataHandlerRegistry.BOOLEAN
    );

    private static final TrackedData<Boolean> HEAR_REACTION_LEFT = DataTracker.registerData(
            DeathAngelEntity.class,
            TrackedDataHandlerRegistry.BOOLEAN
    );



    private static final TrackedData<Boolean> ATTACK_ANIMATION_ACTIVE = DataTracker.registerData(
            DeathAngelEntity.class,
            TrackedDataHandlerRegistry.BOOLEAN
    );

    private static final TrackedData<Boolean> RUN_ATTACK_ANIMATION_ACTIVE = DataTracker.registerData(
            DeathAngelEntity.class,
            TrackedDataHandlerRegistry.BOOLEAN
    );

    private static final TrackedData<Boolean> CHASING_NOISY_TARGET = DataTracker.registerData(
            DeathAngelEntity.class,
            TrackedDataHandlerRegistry.BOOLEAN
    );

    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("IDLE");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("WALK");
    private static final RawAnimation RUN_ANIMATION = RawAnimation.begin().thenLoop("RUN");

    private static final RawAnimation HEAR_LEFT_ANIMATION = RawAnimation.begin().thenPlay("HearLeft");
    private static final RawAnimation HEAR_RIGHT_ANIMATION = RawAnimation.begin().thenPlay("HearRight");

    private static final RawAnimation ATTACK_ANIMATION = RawAnimation.begin().thenPlay("ATTACK");
    private static final RawAnimation RUN_ATTACK_ANIMATION = RawAnimation.begin().thenPlay("RUNATTACK");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final ClimberComponent climberComponent = new ClimberComponent(this);

    private int hearReactionTicks;
    private int attackAnimationTicks;
    private int runAttackAnimationTicks;
    private int attackCooldownTicks;

    private int idleListenCooldownTicks;

    private int suppressHearReactionTicks;
    private UUID noisyTargetUuid;
    private int noisyTargetMemoryTicks;

    private UUID recentHuntTargetUuid;
    private int recentHuntTargetTicks;

    private Vec3d lastKnownSearchPosition;
    private int lastKnownSearchTicks;

    private int ledgeHopCooldownTicks;
    private Vec3d lastLedgeHopPosition;

    public DeathAngelEntity(EntityType<? extends DeathAngelEntity> entityType, World world) {
        super(entityType, world);
        ClimberHelper.initClimber(this);
        this.experiencePoints = 25;
    }

    public static DefaultAttributeContainer.Builder createDeathAngelAttributes() {
        QuietPlaceConfig.DeathAngel config = QuietPlaceConfig.get().deathAngel;

        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, config.maxHealth)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, config.baseMovementSpeed)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, config.attackDamage)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, config.followRange)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, config.knockbackResistance);
    }


    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(HEAR_REACTION_ACTIVE, false);
        builder.add(HEAR_REACTION_LEFT, false);
        builder.add(ATTACK_ANIMATION_ACTIVE, false);
        builder.add(RUN_ATTACK_ANIMATION_ACTIVE, false);
        builder.add(CHASING_NOISY_TARGET, false);
           }

    @Override
    protected void initGoals() {
        QuietPlaceConfig.DeathAngel config = QuietPlaceConfig.get().deathAngel;

        this.goalSelector.add(0, new SwimGoal(this));

        this.goalSelector.add(1, new DeathAngelAttackGoal(this));
        this.goalSelector.add(2, new ChaseNoisyTargetGoal(this, config.chaseSpeed));
        this.goalSelector.add(3, new InvestigateNoiseGoal(this, config.investigateSpeed));
        this.goalSelector.add(4, new SearchLastKnownTargetGoal(this, config.searchSpeed));

        this.goalSelector.add(5, new WanderAroundFarGoal(this, config.wanderSpeed));

        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 16.0f));
        this.goalSelector.add(7, new LookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        ClimberHelper.tickClimber(this);
        ClimberHelper.livingTickClimber(this);

        if (!this.getWorld().isClient()) {
            tickHearReaction();
            tickAttackAnimation();
            tickRunAttackAnimation();
            tickAttackCooldown();
            tickNoisyTargetMemory();
            tickSuppressHearReaction();
            tickRecentHuntTarget();
            tickLastKnownSearchMemory();
            tickLedgeHopAssist();
            tickIdleListening();

        }

        if (!this.getWorld().isClient() && this.age % 10 == 0 && this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                    ParticleTypes.SCULK_SOUL,
                    this.getX(),
                    this.getY() + 1.0,
                    this.getZ(),
                    1,
                    0.25,
                    0.6,
                    0.25,
                    0.005
            );
        }
    }

    private void tickHearReaction() {
        if (this.hearReactionTicks <= 0) {
            return;
        }

        this.hearReactionTicks--;

        if (this.hearReactionTicks <= 0) {
            this.dataTracker.set(HEAR_REACTION_ACTIVE, false);
        }
    }

    private void tickLedgeHopAssist() {
        if (this.ledgeHopCooldownTicks > 0) {
            this.ledgeHopCooldownTicks--;
        }

        if (!this.hasNoisyTargetMemory()) {
            this.lastLedgeHopPosition = this.getPos();
            return;
        }

        if (this.isPlayingHearReaction() || this.isAttackAnimationActive() || this.isRunAttackAnimationActive()) {
            this.lastLedgeHopPosition = this.getPos();
            return;
        }

        if (this.getNavigation().isIdle()) {
            this.lastLedgeHopPosition = this.getPos();
            return;
        }

        if (this.ledgeHopCooldownTicks > 0) {
            this.lastLedgeHopPosition = this.getPos();
            return;
        }

        Vec3d currentPosition = this.getPos();

        if (this.lastLedgeHopPosition == null) {
            this.lastLedgeHopPosition = currentPosition;
            return;
        }

        double horizontalMovedSquared = getHorizontalDistanceSquared(this.lastLedgeHopPosition, currentPosition);
        boolean barelyMoved = horizontalMovedSquared < 0.015 * 0.015;

        /*
         * This is intentionally conservative:
         * only help when it is hunting, trying to path, and barely moving.
         * That usually means it is stuck on a wall/ground transition or ledge lip.
         */
        if (!barelyMoved) {
            this.lastLedgeHopPosition = currentPosition;
            return;
        }

        Vec3d forward = Vec3d.fromPolar(0.0f, this.getYaw()).normalize();

        this.setVelocity(
                this.getVelocity().x + forward.x * 0.18,
                0.34,
                this.getVelocity().z + forward.z * 0.18
        );

        this.velocityDirty = true;
        this.fallDistance = 0.0f;
        this.ledgeHopCooldownTicks = 16;
        this.lastLedgeHopPosition = currentPosition;
    }

    private double getHorizontalDistanceSquared(Vec3d first, Vec3d second) {
        double deltaX = second.x - first.x;
        double deltaZ = second.z - first.z;
        return deltaX * deltaX + deltaZ * deltaZ;
    }



    private void tickAttackAnimation() {
        if (this.attackAnimationTicks <= 0) {
            return;
        }

        this.attackAnimationTicks--;

        if (this.attackAnimationTicks <= 0) {
            this.dataTracker.set(ATTACK_ANIMATION_ACTIVE, false);
        }
    }

    private void tickRunAttackAnimation() {
        if (this.runAttackAnimationTicks <= 0) {
            return;
        }

        this.runAttackAnimationTicks--;

        if (this.runAttackAnimationTicks <= 0) {
            this.dataTracker.set(RUN_ATTACK_ANIMATION_ACTIVE, false);
        }
    }

    private void tickAttackCooldown() {
        if (this.attackCooldownTicks > 0) {
            this.attackCooldownTicks--;
        }
    }



    private void tickRecentHuntTarget() {
        if (this.recentHuntTargetTicks <= 0) {
            this.recentHuntTargetUuid = null;
            return;
        }

        this.recentHuntTargetTicks--;

        if (this.recentHuntTargetTicks <= 0) {
            this.recentHuntTargetUuid = null;
        }
    }

    private void tickSuppressHearReaction() {
        if (this.suppressHearReactionTicks > 0) {
            this.suppressHearReactionTicks--;
        }
    }

    public void suppressHearReaction(int ticks) {
        this.suppressHearReactionTicks = Math.max(this.suppressHearReactionTicks, ticks);
    }

    public boolean shouldSuppressHearReaction() {
        return this.suppressHearReactionTicks > 0;
    }

    private void tickIdleListening() {
        if (this.isPlayingHearReaction()) {
            return;
        }

        if (this.isAttackAnimationActive() || this.isRunAttackAnimationActive()) {
            return;
        }

        if (this.hasNoisyTargetMemory()) {
            return;
        }

        if (this.hasLastKnownSearchPosition()) {
            return;
        }

        if (!this.getNavigation().isIdle()) {
            return;
        }

        if (this.idleListenCooldownTicks > 0) {
            this.idleListenCooldownTicks--;
            return;
        }

        this.idleListenCooldownTicks = 100 + this.getRandom().nextInt(160);

        if (this.getRandom().nextFloat() > 0.45f) {
            return;
        }

        double angle = this.getRandom().nextDouble() * Math.PI * 2.0;
        double distance = 4.0 + this.getRandom().nextDouble() * 6.0;

        Vec3d fakeListenPosition = this.getPos().add(
                Math.cos(angle) * distance,
                0.0,
                Math.sin(angle) * distance
        );

        this.playHearReaction(fakeListenPosition);
    }

    private void tickNoisyTargetMemory() {
        if (this.noisyTargetMemoryTicks <= 0) {
            this.noisyTargetUuid = null;
            this.dataTracker.set(CHASING_NOISY_TARGET, false);
            return;
        }

        this.noisyTargetMemoryTicks--;

        if (this.noisyTargetMemoryTicks <= 0) {
            this.noisyTargetUuid = null;
            this.dataTracker.set(CHASING_NOISY_TARGET, false);
        }
    }
    private void tickLastKnownSearchMemory() {
        if (this.lastKnownSearchTicks <= 0) {
            this.lastKnownSearchPosition = null;
            return;
        }

        this.lastKnownSearchTicks--;

        if (this.lastKnownSearchTicks <= 0) {
            this.lastKnownSearchPosition = null;
        }
    }

    public void rememberLastKnownSearchPosition(Vec3d position, int ticks) {
        if (position == null || ticks <= 0) {
            return;
        }

        this.lastKnownSearchPosition = position;
        this.lastKnownSearchTicks = Math.max(this.lastKnownSearchTicks, ticks);
    }

    public boolean hasLastKnownSearchPosition() {
        return this.lastKnownSearchPosition != null && this.lastKnownSearchTicks > 0;
    }

    public Vec3d getLastKnownSearchPosition() {
        return this.lastKnownSearchPosition;
    }

    public void playHearReaction(Vec3d noisePosition) {
        Vec3d lookDirection = this.getRotationVec(1.0f).normalize();
        Vec3d directionToNoise = noisePosition.subtract(this.getPos()).normalize();

        double cross = lookDirection.x * directionToNoise.z - lookDirection.z * directionToNoise.x;
        boolean noiseIsLeft = cross > 0.0;

        /*
         * Our animation selection is intentionally swapped:
         * noiseIsLeft=true currently plays HearRight,
         * noiseIsLeft=false currently plays HearLeft.
         *
         * HearRight is about 3 seconds.
         * HearLeft is about 4 seconds.
         */
        this.hearReactionTicks = noiseIsLeft ? 20 * 3 : 20 * 4;

        this.dataTracker.set(HEAR_REACTION_LEFT, noiseIsLeft);
        this.dataTracker.set(HEAR_REACTION_ACTIVE, true);
    }


    @Override
    protected EntityNavigation createNavigation(World world) {
        ClimberPathNavigator<DeathAngelEntity> navigator = new ClimberPathNavigator<>(this, world, false);
        navigator.setCanSwim(true);
        return navigator;
    }

    @Override
    public float getMovementSpeed() {
        return (float) this.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
    }

    @Override
    public boolean canClimbOnBlock(BlockState state, BlockPos pos) {
        return !state.getCollisionShape(this.getWorld(), pos).isEmpty();
    }

    @Override
    public ClimberComponent getClimberComponent() {
        return this.climberComponent;
    }

    @Override
    public BlockPos getBlockPos() {
        return super.getBlockPos();
    }

    @Override
    public float getBlockSlipperiness(BlockPos pos) {
        return this.getWorld().getBlockState(pos).getBlock().getSlipperiness();
    }

    @Override
    public HostileEntity asMob() {
        return this;
    }

    @Override
    public void setLerpYRot(Float yRot) {
        if (yRot != null) {
            this.setYaw(yRot);
        }
    }

    @Override
    public void setLerpXRot(Float xRot) {
        if (xRot != null) {
            this.setPitch(xRot);
        }
    }

    @Override
    public void setLerpYHeadRot(Float yHeadRot) {
        if (yHeadRot != null) {
            this.headYaw = yHeadRot;
        }
    }

    @Override
    public void setLerpHeadSteps(int steps) {
        // Not needed for our first test.
    }

    @Override
    public void travel(Vec3d movementInput) {
        if (ClimberHelper.handleTravel(this, movementInput)) {
            return;
        }

        super.travel(movementInput);
        ClimberHelper.postTravel(this, movementInput);
    }

    @Override
    public void move(MovementType movementType, Vec3d movement) {
        if (ClimberHelper.handleMove(this, movementType, movement, true)) {
            return;
        }

        super.move(movementType, movement);
        ClimberHelper.handleMove(this, movementType, movement, false);
    }

    @Override
    public void jump() {
        if (ClimberHelper.handleJump(this)) {
            return;
        }

        super.jump();
    }

    public void clearHuntAndSearchMemory() {
        this.noisyTargetUuid = null;
        this.noisyTargetMemoryTicks = 0;

        this.recentHuntTargetUuid = null;
        this.recentHuntTargetTicks = 0;

        this.lastKnownSearchPosition = null;
        this.lastKnownSearchTicks = 0;

        this.suppressHearReactionTicks = 0;

        this.getNavigation().stop();
    }


    public void startAttackAnimation(int ticks) {
        this.attackAnimationTicks = Math.max(1, ticks);
        this.dataTracker.set(ATTACK_ANIMATION_ACTIVE, true);
    }

    public void startRunAttackAnimation(int ticks) {
        this.runAttackAnimationTicks = Math.max(1, ticks);
        this.dataTracker.set(RUN_ATTACK_ANIMATION_ACTIVE, true);
    }

    public boolean isAttackAnimationActive() {
        return this.dataTracker.get(ATTACK_ANIMATION_ACTIVE);
    }

    public boolean isRunAttackAnimationActive() {
        return this.dataTracker.get(RUN_ATTACK_ANIMATION_ACTIVE);
    }

    public boolean isChasingNoisyTargetClientSynced() {
        return this.dataTracker.get(CHASING_NOISY_TARGET);
    }

    public boolean canAttackNow() {
        return this.attackCooldownTicks <= 0;
    }

    public void setAttackCooldown(int ticks) {
        this.attackCooldownTicks = Math.max(this.attackCooldownTicks, ticks);
    }

    public void rememberNoisyTarget(UUID targetUuid, int memoryTicks) {
        if (targetUuid == null || memoryTicks <= 0) {
            return;
        }

        this.noisyTargetUuid = targetUuid;
        this.noisyTargetMemoryTicks = Math.max(this.noisyTargetMemoryTicks, memoryTicks);
        this.dataTracker.set(CHASING_NOISY_TARGET, true);

        this.recentHuntTargetUuid = targetUuid;
        this.recentHuntTargetTicks = Math.max(this.recentHuntTargetTicks, 20 * 10);
    }

    public boolean canHuntEntity(UUID entityUuid) {
        if (entityUuid == null) {
            return false;
        }

        Entity entity = this.getWorld() instanceof ServerWorld serverWorld
                ? serverWorld.getEntity(entityUuid)
                : null;

        return this.canHuntEntity(entity);
    }

    public boolean canHuntEntity(Entity entity) {
        if (!(entity instanceof PlayerEntity player)) {
            return false;
        }

        if (!player.isAlive()) {
            return false;
        }

        if (player.isSpectator()) {
            return false;
        }

        if (player.isCreative() && !QuietPlaceConfig.get().deathAngel.huntCreativePlayers) {
            return false;
        }

        return true;
    }

    public boolean isRecentHuntTarget(UUID targetUuid) {
        return targetUuid != null
                && targetUuid.equals(this.recentHuntTargetUuid)
                && this.recentHuntTargetTicks > 0;
    }

    public boolean isActivelyHuntingOrSearching() {
        return this.hasNoisyTargetMemory()
                || this.hasLastKnownSearchPosition()
                || this.isAttackAnimationActive()
                || this.isRunAttackAnimationActive()
                || this.shouldSuppressHearReaction();
    }



    public boolean shouldPlayHearReactionForHunt(UUID targetUuid) {
        if (targetUuid == null) {
            return true;
        }

        if (this.shouldSuppressHearReaction()) {
            return false;
        }

        if (this.isRecentHuntTarget(targetUuid)) {
            return false;
        }

        return true;
    }



    public boolean hasNoisyTargetMemory() {
        return this.noisyTargetUuid != null && this.noisyTargetMemoryTicks > 0;
    }

    public Entity getNoisyTargetEntity() {
        if (this.noisyTargetUuid == null) {
            return null;
        }

        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return null;
        }

        return serverWorld.getEntity(this.noisyTargetUuid);
    }

    public int getHearReactionTicksRemaining() {
        return this.hearReactionTicks;
    }

    public boolean isPlayingHearReaction() {
        return this.dataTracker.get(HEAR_REACTION_ACTIVE);
    }

    public boolean isHearReactionLeft() {
        return this.dataTracker.get(HEAR_REACTION_LEFT);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 5, state -> {
            if (this.isRunAttackAnimationActive()) {
                return state.setAndContinue(RUN_ATTACK_ANIMATION);
            }

            if (this.isAttackAnimationActive()) {
                return state.setAndContinue(ATTACK_ANIMATION);
            }

            if (this.isPlayingHearReaction()) {
                if (this.isHearReactionLeft()) {
                    return state.setAndContinue(HEAR_RIGHT_ANIMATION);
                }

                return state.setAndContinue(HEAR_LEFT_ANIMATION);
            }

            if (state.isMoving()) {
                if (this.isChasingNoisyTargetClientSynced()) {
                    return state.setAndContinue(RUN_ANIMATION);
                }

                return state.setAndContinue(WALK_ANIMATION);
            }

            return state.setAndContinue(IDLE_ANIMATION);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}