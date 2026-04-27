package net.droingo.aquietplace.entity.deathangel;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class DeathAngelEntity extends HostileEntity implements GeoEntity {
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("IDLE");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("WALK");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

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
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));

        // Temporary basic movement so we can confirm pathfinding and animations work.
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.85));

        // Temporary visual behavior. Later, the Death Angel will mostly react to sound instead of sight.
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 16.0f));
        this.goalSelector.add(7, new LookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();

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

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 5, state -> {
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