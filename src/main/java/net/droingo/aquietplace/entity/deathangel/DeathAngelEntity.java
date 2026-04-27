package net.droingo.aquietplace.entity.deathangel;

import net.droingo.aquietplace.entity.deathangel.goal.InvestigateNoiseGoal;
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

public class DeathAngelEntity extends HostileEntity implements GeoEntity {
    private static final TrackedData<Boolean> HEAR_REACTION_ACTIVE = DataTracker.registerData(
            DeathAngelEntity.class,
            TrackedDataHandlerRegistry.BOOLEAN
    );

    private static final TrackedData<Boolean> HEAR_REACTION_LEFT = DataTracker.registerData(
            DeathAngelEntity.class,
            TrackedDataHandlerRegistry.BOOLEAN
    );

    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("IDLE");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("WALK");
    private static final RawAnimation HEAR_LEFT_ANIMATION = RawAnimation.begin().thenPlay("HearLeft");
    private static final RawAnimation HEAR_RIGHT_ANIMATION = RawAnimation.begin().thenPlay("HearRight");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private int hearReactionTicks;

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
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));

        // First real sound-hunting behavior.
        this.goalSelector.add(2, new InvestigateNoiseGoal(this, 1.15));

        // Temporary basic movement so we can confirm pathfinding and animations work.
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.85));

        // Temporary visual behavior. Later, the Death Angel will mostly react to sound instead of sight.
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 16.0f));
        this.goalSelector.add(7, new LookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.getWorld().isClient() && this.hearReactionTicks > 0) {
            this.hearReactionTicks--;

            if (this.hearReactionTicks <= 0) {
                this.dataTracker.set(HEAR_REACTION_ACTIVE, false);
            }
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

    public void playHearReaction(Vec3d noisePosition) {
        Vec3d lookDirection = this.getRotationVec(1.0f).normalize();
        Vec3d directionToNoise = noisePosition.subtract(this.getPos()).normalize();

        double cross = lookDirection.x * directionToNoise.z - lookDirection.z * directionToNoise.x;
        boolean noiseIsLeft = cross > 0.0;

        this.hearReactionTicks = 40;
        this.dataTracker.set(HEAR_REACTION_LEFT, noiseIsLeft);
        this.dataTracker.set(HEAR_REACTION_ACTIVE, true);
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
            if (this.isPlayingHearReaction()) {
                if (this.isHearReactionLeft()) {
                    return state.setAndContinue(HEAR_RIGHT_ANIMATION);
                }

                return state.setAndContinue(HEAR_LEFT_ANIMATION);
            }

            if (state.isMoving()) {
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