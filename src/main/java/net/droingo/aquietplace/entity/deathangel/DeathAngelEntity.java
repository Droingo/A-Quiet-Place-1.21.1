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

import java.util.UUID;

public class DeathAngelEntity extends HostileEntity implements GeoEntity {
    private static final TrackedData<Boolean> HEAR_REACTION_ACTIVE = DataTracker.registerData(
            DeathAngelEntity.class,
            TrackedDataHandlerRegistry.BOOLEAN
    );

    private static final TrackedData<Boolean> HEAR_REACTION_LEFT = DataTracker.registerData(
            DeathAngelEntity.class,
            TrackedDataHandlerRegistry.BOOLEAN
    );

    private static final TrackedData<Boolean> CLIMBING = DataTracker.registerData(
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

    private int hearReactionTicks;
    private int attackAnimationTicks;
    private int runAttackAnimationTicks;
    private int attackCooldownTicks;

    private static final double CLIMB_UP_SPEED = 0.22;
    private static final double CLIMB_FORWARD_SPEED = 0.08;
    private static final double LEDGE_FORWARD_SPEED = 0.24;
    private static final double LEDGE_UP_SPEED = 0.12;
    private static final int CLIMB_MEMORY_TICKS = 8;

    private int climbTicks;
    private Vec3d climbTargetPosition;

    private int suppressHearReactionTicks;
    private UUID noisyTargetUuid;
    private int noisyTargetMemoryTicks;

    private UUID recentHuntTargetUuid;
    private int recentHuntTargetTicks;

    public DeathAngelEntity(EntityType<? extends DeathAngelEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 25;
    }

    public static DefaultAttributeContainer.Builder createDeathAngelAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 80.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.30)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 12.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8);
    }


    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(HEAR_REACTION_ACTIVE, false);
        builder.add(HEAR_REACTION_LEFT, false);
        builder.add(ATTACK_ANIMATION_ACTIVE, false);
        builder.add(RUN_ATTACK_ANIMATION_ACTIVE, false);
        builder.add(CHASING_NOISY_TARGET, false);
        builder.add(CLIMBING, false);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));

        // Highest priority: attack a noisy target if close enough.
        this.goalSelector.add(1, new DeathAngelAttackGoal(this));

        // If the Death Angel remembers a noisy player, chase them.
        this.goalSelector.add(2, new ChaseNoisyTargetGoal(this, 1.35));

        // Otherwise, investigate sound positions.
        this.goalSelector.add(3, new InvestigateNoiseGoal(this, 1.15));

        // Temporary basic movement so we can confirm pathfinding and animations work.
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.85));

        // Temporary visual behavior. Later, the Death Angel will mostly react to sound instead of sight.
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 16.0f));
        this.goalSelector.add(7, new LookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.getWorld().isClient()) {
            tickHearReaction();
            tickAttackAnimation();
            tickRunAttackAnimation();
            tickAttackCooldown();
            tickNoisyTargetMemory();
            tickSuppressHearReaction();
            tickRecentHuntTarget();
            tickWallClimbing();
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

    private void tickWallClimbing() {
        boolean shouldClimb = shouldClimbWall();

        if (shouldClimb) {
            this.climbTicks = CLIMB_MEMORY_TICKS;
        } else if (this.climbTicks > 0) {
            this.climbTicks--;
        }

        boolean climbing = this.climbTicks > 0;
        this.dataTracker.set(CLIMBING, climbing);

        if (!climbing) {
            return;
        }

        this.fallDistance = 0.0f;

        if (this.isTouchingWater()) {
            return;
        }

        Vec3d climbDirection = getHorizontalDirectionToClimbTarget();

        double pushX = this.getVelocity().x * 0.5;
        double pushZ = this.getVelocity().z * 0.5;
        double pushY = CLIMB_UP_SPEED;

        if (climbDirection != null) {
            pushX = climbDirection.x * CLIMB_FORWARD_SPEED;
            pushZ = climbDirection.z * CLIMB_FORWARD_SPEED;
        }

        /*
         * Ledge pull-over:
         * If the lower body is still near a wall but the upper body has space,
         * stop trying to climb straight up and push forward over the lip.
         */
        if (isAtClimbLedge() && climbDirection != null) {
            pushX = climbDirection.x * LEDGE_FORWARD_SPEED;
            pushZ = climbDirection.z * LEDGE_FORWARD_SPEED;
            pushY = LEDGE_UP_SPEED;
        }

        this.setVelocity(pushX, pushY, pushZ);
        this.velocityDirty = true;
    }

    private boolean shouldClimbWall() {
        if (this.isTouchingWater()) {
            return false;
        }

        if (!this.hasNoisyTargetMemory() && this.getNavigation().isIdle()) {
            return false;
        }

        return this.horizontalCollision || hasClimbableBlockNearby();
    }

    private Vec3d getHorizontalDirectionToClimbTarget() {
        if (this.climbTargetPosition == null) {
            return null;
        }
        //comment

        Vec3d directionToTarget = this.climbTargetPosition.subtract(this.getPos());
        double horizontalLength = Math.sqrt(
                directionToTarget.x * directionToTarget.x
                        + directionToTarget.z * directionToTarget.z
        );

        if (horizontalLength <= 0.001) {
            return null;
        }

        return new Vec3d(
                directionToTarget.x / horizontalLength,
                0.0,
                directionToTarget.z / horizontalLength
        );
    }

    private boolean isAtClimbLedge() {
        Vec3d climbDirection = getHorizontalDirectionToClimbTarget();

        if (climbDirection == null) {
            return false;
        }

        double frontX = this.getX() + climbDirection.x * 0.85;
        double frontZ = this.getZ() + climbDirection.z * 0.85;

        boolean lowerBlocked = hasSolidCollisionAt(frontX, this.getY() + 0.4, frontZ);
        boolean middleBlocked = hasSolidCollisionAt(frontX, this.getY() + 1.2, frontZ);
        boolean upperBlocked = hasSolidCollisionAt(frontX, this.getY() + 2.2, frontZ);

        boolean spaceAboveSelf = !hasSolidCollisionAt(this.getX(), this.getY() + 2.7, this.getZ());
        boolean spaceAheadUpper = !upperBlocked;

        /*
         * Lower or middle body is still touching the wall,
         * but the space near the top/front is open.
         * That usually means we are at the ledge lip.
         */
        return (lowerBlocked || middleBlocked) && spaceAboveSelf && spaceAheadUpper;
    }

    private boolean hasClimbableBlockNearby() {
        double checkDistance = 0.75;

        return hasSolidCollisionAt(this.getX() + checkDistance, this.getY() + 0.5, this.getZ())
                || hasSolidCollisionAt(this.getX() - checkDistance, this.getY() + 0.5, this.getZ())
                || hasSolidCollisionAt(this.getX(), this.getY() + 0.5, this.getZ() + checkDistance)
                || hasSolidCollisionAt(this.getX(), this.getY() + 0.5, this.getZ() - checkDistance)
                || hasSolidCollisionAt(this.getX() + checkDistance, this.getY() + 1.6, this.getZ())
                || hasSolidCollisionAt(this.getX() - checkDistance, this.getY() + 1.6, this.getZ())
                || hasSolidCollisionAt(this.getX(), this.getY() + 1.6, this.getZ() + checkDistance)
                || hasSolidCollisionAt(this.getX(), this.getY() + 1.6, this.getZ() - checkDistance);
    }

    private boolean hasSolidCollisionAt(double x, double y, double z) {
        BlockPos blockPos = BlockPos.ofFloored(x, y, z);
        BlockState blockState = this.getWorld().getBlockState(blockPos);

        return !blockState.getCollisionShape(this.getWorld(), blockPos).isEmpty();
    }

    public void setClimbTargetPosition(Vec3d climbTargetPosition) {
        this.climbTargetPosition = climbTargetPosition;
    }

    public void clearClimbTargetPosition() {
        this.climbTargetPosition = null;
    }

    public boolean isDeathAngelClimbing() {
        return this.dataTracker.get(CLIMBING);
    }

    @Override
    public boolean isClimbing() {
        return super.isClimbing() || this.isDeathAngelClimbing();
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

    public boolean shouldPlayHearReactionForHunt(UUID targetUuid) {
        if (targetUuid == null) {
            return true;
        }

        if (this.shouldSuppressHearReaction()) {
            return false;
        }

        if (targetUuid.equals(this.recentHuntTargetUuid) && this.recentHuntTargetTicks > 0) {
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

            if (state.isMoving() || this.isDeathAngelClimbing()) {
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