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
import com.nyfaria.awcapi.ClimberHelper;
import net.minecraft.entity.MovementType;
import com.nyfaria.awcapi.entity.movement.ClimberPathNavigator;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.droingo.aquietplace.entity.deathangel.goal.SearchLastKnownTargetGoal;
import net.droingo.aquietplace.config.QuietPlaceConfig;
import net.droingo.aquietplace.registry.ModSounds;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.block.Block;
import net.minecraft.util.math.MathHelper;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;

import java.util.UUID;

public class DeathAngelEntity extends HostileEntity implements GeoEntity, DeathAngelAwcCompatibilityBridge {
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
    private int idleBreathCooldownTicks;
    private int suppressHearReactionTicks;
    private UUID noisyTargetUuid;
    private int noisyTargetMemoryTicks;

    private UUID recentHuntTargetUuid;
    private int recentHuntTargetTicks;

    private Vec3d lastKnownSearchPosition;
    private int lastKnownSearchTicks;

    private int ledgeHopCooldownTicks;
    private int stuckMovementTicks;
    private Vec3d lastLedgeHopPosition;

    private int verticalChaseLeapCooldownTicks;
    private int verticalChaseLeapChargeTicks;
    private int leapFacingLockTicks;
    private float leapLockedYaw;
    private int movementAnimationGraceTicks;
    private Vec3d lastAnimationMovementPosition;


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
            tickVerticalChaseLeapAssist();
            tickIdleBreathing();
            tickIdleListening();

        }
        tickMovementAnimationSmoothing();
        tickLeapFacingLock();

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

    private void tickVerticalChaseLeapAssist() {
        if (this.verticalChaseLeapCooldownTicks > 0) {
            this.verticalChaseLeapCooldownTicks--;
        }

        if (!this.hasNoisyTargetMemory()) {
            resetVerticalChaseLeapTracking();
            return;
        }

        if (this.isPlayingHearReaction() || this.isAttackAnimationActive() || this.isRunAttackAnimationActive()) {
            resetVerticalChaseLeapTracking();
            return;
        }

        if (this.verticalChaseLeapCooldownTicks > 0) {
            return;
        }

        Entity target = this.getNoisyTargetEntity();

        if (!this.canHuntEntity(target)) {
            resetVerticalChaseLeapTracking();
            return;
        }

        double horizontalDistanceSquared = getHorizontalDistanceSquared(this.getPos(), target.getPos());
        double verticalDifference = target.getY() - this.getY();
        double absoluteVerticalDifference = Math.abs(verticalDifference);

        /*
         * Only leap for real height problems.
         * This avoids triggering on normal hills, stairs, slabs, and one-block terrain.
         */
        if (absoluteVerticalDifference < 3.5) {
            resetVerticalChaseLeapTracking();
            return;
        }

        /*
         * Do not leap from absurd distances.
         * We allow close horizontal leaps because the player may be directly above
         * or below the Death Angel on a ledge/building.
         */
        if (horizontalDistanceSquared > 576.0) {
            resetVerticalChaseLeapTracking();
            return;
        }

        /*
         * Give normal pathfinding a short chance first.
         * If the height difference stays large for several ticks, leap.
         */
        this.verticalChaseLeapChargeTicks++;

        if (this.verticalChaseLeapChargeTicks < 6) {
            return;
        }

        leapTowardVerticalTarget(target, verticalDifference);

        this.verticalChaseLeapCooldownTicks = 60;
        this.verticalChaseLeapChargeTicks = 0;
    }

    private void resetVerticalChaseLeapTracking() {
        this.verticalChaseLeapChargeTicks = 0;
    }

    private void leapTowardVerticalTarget(Entity target, double verticalDifference) {
        Vec3d directionToTarget = target.getPos().subtract(this.getPos());
        Vec3d flatDirection = new Vec3d(directionToTarget.x, 0.0, directionToTarget.z);

        if (flatDirection.lengthSquared() < 0.001) {
            return;
        }

        flatDirection = flatDirection.normalize();

        breakSimpleBlocksForLeap(flatDirection);

        /*
         * Big chase leap.
         * Horizontal strength should cover roughly several blocks.
         * Vertical strength is higher when the target is above.
         */
        double horizontalStrength = 1.25;
        double verticalStrength = verticalDifference > 0.0 ? 0.72 : 0.42;

        this.setVelocity(
                flatDirection.x * horizontalStrength,
                verticalStrength,
                flatDirection.z * horizontalStrength
        );

        this.velocityDirty = true;
        this.fallDistance = 0.0f;

        lockLeapFacing(flatDirection, 14);
        this.getLookControl().lookAt(target, 70.0f, 70.0f);
        this.suppressHearReaction(12);

        /*
         * Keep run/walk animation alive during the leap.
         * Later this can trigger a real leap animation.
         */
        this.movementAnimationGraceTicks = Math.max(this.movementAnimationGraceTicks, 18);

        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                    ParticleTypes.SONIC_BOOM,
                    this.getX(),
                    this.getY() + 1.0,
                    this.getZ(),
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            );

            playDeathAngelSound(
                    ModSounds.DEATH_ANGEL_INVESTIGATE_GROWL,
                    1.0f,
                    1.15f
            );
        }
    }


    private void breakSimpleBlocksForLeap(Vec3d flatDirection) {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        BlockPos basePos = this.getBlockPos();

        for (int forward = 1; forward <= 2; forward++) {
            int offsetX = (int) Math.round(flatDirection.x * forward);
            int offsetZ = (int) Math.round(flatDirection.z * forward);

            BlockPos center = basePos.add(offsetX, 0, offsetZ);

            for (int y = 0; y <= 2; y++) {
                tryBreakLeapBlock(serverWorld, center.up(y));
            }
        }
    }

    private void tryBreakLeapBlock(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);

        if (!canBreakDuringLeap(state, pos)) {
            return;
        }

        world.breakBlock(pos, false, this);
    }

    private boolean canBreakDuringLeap(BlockState state, BlockPos pos) {
        if (state.isAir()) {
            return false;
        }

        if (state.hasBlockEntity()) {
            return false;
        }

        if (state.getHardness(this.getWorld(), pos) < 0.0f) {
            return false;
        }

        Block block = state.getBlock();

        if (block == Blocks.CHEST || block == Blocks.BARREL || block == Blocks.SHULKER_BOX) {
            return false;
        }

        return state.isIn(BlockTags.LEAVES)
                || state.isIn(BlockTags.LOGS)
                || state.isIn(BlockTags.PLANKS);
    }

    private void lockLeapFacing(Vec3d flatDirection, int ticks) {
        if (flatDirection.lengthSquared() < 0.001) {
            return;
        }

        float yaw = getYawFromFlatDirection(flatDirection);

        this.leapLockedYaw = yaw;
        this.leapFacingLockTicks = Math.max(this.leapFacingLockTicks, ticks);

        applyLeapYaw(yaw);
    }

    private void tickLeapFacingLock() {
        if (this.leapFacingLockTicks <= 0) {
            return;
        }

        applyLeapYaw(this.leapLockedYaw);
        this.leapFacingLockTicks--;
    }

    private float getYawFromFlatDirection(Vec3d flatDirection) {
        return (float) (MathHelper.atan2(flatDirection.z, flatDirection.x) * 180.0F / Math.PI) - 90.0F;
    }

    private void applyLeapYaw(float yaw) {
        this.setYaw(yaw);
        this.bodyYaw = yaw;
        this.headYaw = yaw;

        this.prevYaw = yaw;
        this.prevBodyYaw = yaw;
        this.prevHeadYaw = yaw;
    }






    private void tickMovementAnimationSmoothing() {
        Vec3d currentPosition = this.getPos();

        if (this.lastAnimationMovementPosition == null) {
            this.lastAnimationMovementPosition = currentPosition;
            return;
        }

        double horizontalMovedSquared = getHorizontalDistanceSquared(this.lastAnimationMovementPosition, currentPosition);
        boolean actuallyMoved = horizontalMovedSquared > 0.003 * 0.003;

        if (actuallyMoved) {
            this.movementAnimationGraceTicks = 8;
        } else if (this.movementAnimationGraceTicks > 0) {
            this.movementAnimationGraceTicks--;
        }

        this.lastAnimationMovementPosition = currentPosition;
    }

    private boolean shouldKeepMovementAnimationActive() {
        return this.movementAnimationGraceTicks > 0
                || this.getVelocity().horizontalLengthSquared() > 0.003 * 0.003;
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
            resetLedgeHopTracking();
            return;
        }

        if (this.isPlayingHearReaction() || this.isAttackAnimationActive() || this.isRunAttackAnimationActive()) {
            resetLedgeHopTracking();
            return;
        }

        if (this.getNavigation().isIdle()) {
            resetLedgeHopTracking();
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

        /*
         * Only count as stuck when it is almost completely failing to move.
         * This avoids firing the helper during normal step-up/step-down movement.
         */
        boolean barelyMoved = horizontalMovedSquared < 0.01 * 0.01;

        if (!barelyMoved) {
            this.stuckMovementTicks = 0;
            this.lastLedgeHopPosition = currentPosition;
            return;
        }

        this.stuckMovementTicks++;

        /*
         * Wait about half a second before helping.
         * This prevents the helper from fighting normal pathfinding on uneven terrain.
         */
        if (this.stuckMovementTicks < 10) {
            this.lastLedgeHopPosition = currentPosition;
            return;
        }

        Vec3d forward = getLedgeHopDirection();

        this.setVelocity(
                this.getVelocity().x + forward.x * 0.12,
                0.26,
                this.getVelocity().z + forward.z * 0.12
        );

        this.velocityDirty = true;
        this.fallDistance = 0.0f;
        this.ledgeHopCooldownTicks = 24;
        this.stuckMovementTicks = 0;
        this.lastLedgeHopPosition = currentPosition;
    }

    private double getHorizontalDistanceSquared(Vec3d first, Vec3d second) {
        double deltaX = second.x - first.x;
        double deltaZ = second.z - first.z;
        return deltaX * deltaX + deltaZ * deltaZ;
    }

    private void resetLedgeHopTracking() {
        this.stuckMovementTicks = 0;
        this.lastLedgeHopPosition = this.getPos();
    }

    private Vec3d getLedgeHopDirection() {
        BlockPos navigationTarget = this.getNavigation().getTargetPos();

        if (navigationTarget != null) {
            Vec3d directionToTarget = navigationTarget.toCenterPos().subtract(this.getPos());

            if (directionToTarget.horizontalLengthSquared() > 0.001) {
                return new Vec3d(directionToTarget.x, 0.0, directionToTarget.z).normalize();
            }
        }

        return Vec3d.fromPolar(0.0f, this.getYaw()).normalize();
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

        if (!this.getWorld().isClient()) {
            playDeathAngelSound(
                    ModSounds.DEATH_ANGEL_LISTEN_TWITCH,
                    0.8f,
                    0.85f + this.getRandom().nextFloat() * 0.25f
            );
        }
    }


    @Override
    protected EntityNavigation createNavigation(World world) {
        ClimberPathNavigator<DeathAngelEntity> navigator = new ClimberPathNavigator<>(this, world, false);
        navigator.setCanSwim(true);
        return navigator;
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
    public HostileEntity asMob() {
        return this;
    }


    public float getAwcMovementSpeedValue() {
        return (float) this.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
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
    public float getBlockSlipperiness(BlockPos pos) {
        return this.getWorld().getBlockState(pos).getBlock().getSlipperiness();
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
        // Not needed for the Death Angel.
    }

    @Override
    public void move(MovementType movementType, Vec3d movement) {
        if (ClimberHelper.handleMove(this, movementType, movement, true)) {
            return;
        }

        super.move(movementType, movement);
        ClimberHelper.handleMove(this, movementType, movement, false);
    }
    private void tickIdleBreathing() {
        if (this.isDead() || !this.isAlive()) {
            return;
        }

        if (this.isPlayingHearReaction()) {
            return;
        }

        if (this.isAttackAnimationActive() || this.isRunAttackAnimationActive()) {
            return;
        }

        if (this.idleBreathCooldownTicks > 0) {
            this.idleBreathCooldownTicks--;
            return;
        }

        boolean activelyHunting = this.hasNoisyTargetMemory() || this.hasLastKnownSearchPosition();

        if (activelyHunting) {
            this.idleBreathCooldownTicks = 70 + this.getRandom().nextInt(90);
        } else {
            this.idleBreathCooldownTicks = 120 + this.getRandom().nextInt(180);
        }

        float volume = activelyHunting ? 0.65f : 0.45f;
        float pitch = 0.85f + this.getRandom().nextFloat() * 0.25f;

        playDeathAngelSound(ModSounds.DEATH_ANGEL_IDLE_BREATH, volume, pitch);
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

        if (!this.getWorld().isClient()) {
            playDeathAngelSound(
                    ModSounds.DEATH_ANGEL_ATTACK_WINDUP,
                    0.9f,
                    0.9f + this.getRandom().nextFloat() * 0.15f
            );
        }
    }

    public void startRunAttackAnimation(int ticks) {
        this.runAttackAnimationTicks = Math.max(1, ticks);
        this.dataTracker.set(RUN_ATTACK_ANIMATION_ACTIVE, true);

        if (!this.getWorld().isClient()) {
            playDeathAngelSound(
                    ModSounds.DEATH_ANGEL_ATTACK_WINDUP,
                    1.0f,
                    0.75f + this.getRandom().nextFloat() * 0.15f
            );
        }
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
    public boolean damage(DamageSource source, float amount) {
        boolean damaged = super.damage(source, amount);

        if (damaged && !this.getWorld().isClient()) {
            playDeathAngelSound(
                    ModSounds.DEATH_ANGEL_HURT,
                    0.9f,
                    0.85f + this.getRandom().nextFloat() * 0.25f
            );
        }

        return damaged;
    }

    public void playAttackHitSound() {
        if (this.getWorld().isClient()) {
            return;
        }

        playDeathAngelSound(
                ModSounds.DEATH_ANGEL_ATTACK_HIT,
                1.0f,
                0.85f + this.getRandom().nextFloat() * 0.2f
        );
    }

    public void playInvestigateGrowlSound() {
        if (this.getWorld().isClient()) {
            return;
        }

        playDeathAngelSound(
                ModSounds.DEATH_ANGEL_INVESTIGATE_GROWL,
                0.9f,
                0.85f + this.getRandom().nextFloat() * 0.25f
        );
    }

    private void playDeathAngelSound(SoundEvent soundEvent, float volume, float pitch) {
        this.getWorld().playSound(
                null,
                this.getX(),
                this.getY(),
                this.getZ(),
                soundEvent,
                SoundCategory.HOSTILE,
                volume,
                pitch
        );
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

            boolean shouldPlayMovementAnimation = state.isMoving() || this.shouldKeepMovementAnimationActive();

            if (shouldPlayMovementAnimation) {
                if (this.isChasingNoisyTargetClientSynced() || this.hasNoisyTargetMemory()) {
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