package net.droingo.aquietplace.block.entity;

import net.droingo.aquietplace.block.GlassBottleTrapBlock;
import net.droingo.aquietplace.config.QuietPlaceConfig;
import net.droingo.aquietplace.noise.NoiseEvent;
import net.droingo.aquietplace.noise.NoiseSystem;
import net.droingo.aquietplace.noise.NoiseType;
import net.droingo.aquietplace.registry.ModBlockEntities;
import net.droingo.aquietplace.registry.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.droingo.aquietplace.entity.deathangel.DeathAngelEntity;
import net.droingo.aquietplace.registry.ModEntities;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.droingo.aquietplace.network.GlassBottleTrapOpenDisarmPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.UUID;

import java.util.List;

public class GlassBottleTrapBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation IDLE_SWING = RawAnimation.begin().thenPlay("idle_swing");
    private static final RawAnimation TRIGGERED_0 = RawAnimation.begin().thenPlay("triggered_0");
    private static final RawAnimation TRIGGERED_1 = RawAnimation.begin().thenPlay("triggered_1");

    private static final int DISARM_REQUIRED_CLICKS = 3;
    private static final int DISARM_TIME_LIMIT_TICKS = 32;
    private static final double DISARM_MAX_DISTANCE_SQUARED = 36.0;

    private UUID disarmingPlayerUuid = null;
    private int disarmClicks = 0;
    private int disarmTicksRemaining = 0;

    private int idleSwingCooldownTicks = 40;
    private int idleSwingTicks = 0;

    private int triggerStage = 0;
    private int triggerTicks = 0;
    private boolean emittedNoise = false;

    private boolean disarmReady = false;

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public GlassBottleTrapBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GLASS_BOTTLE_TRAP, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, GlassBottleTrapBlockEntity blockEntity) {
        if (world.isClient()) {
            blockEntity.tickClientIdleAnimation(world, state);
            return;
        }

        if (world instanceof ServerWorld serverWorld) {
            blockEntity.tickServer(serverWorld, pos, state);
        }
    }

    private void tickClientIdleAnimation(World world, BlockState state) {
        int stage = getStageFromState(state);

        if (stage != 0) {
            this.idleSwingTicks = 0;
            return;
        }

        if (this.idleSwingTicks > 0) {
            this.idleSwingTicks--;
            return;
        }

        if (this.idleSwingCooldownTicks > 0) {
            this.idleSwingCooldownTicks--;
            return;
        }

        this.idleSwingTicks = 35;
        this.idleSwingCooldownTicks = 80 + world.getRandom().nextInt(120);
    }

    private void tickServer(ServerWorld world, BlockPos pos, BlockState state) {
        QuietPlaceConfig.GlassBottleTrap config = QuietPlaceConfig.get().glassBottleTrap;

        if (!config.enabled) {
            return;
        }

        if (hasActiveDisarmSession()) {
            tickDisarmSession(world, pos, config);
            return;
        }

        if (this.triggerStage == 0) {
            checkEntityTrigger(world, pos, config);
            return;
        }

        tickTriggered(world, pos, config);
    }

    private void checkEntityTrigger(ServerWorld world, BlockPos pos, QuietPlaceConfig.GlassBottleTrap config) {
        double radius = Math.max(0.1, config.triggerRadius);

        Box triggerBox = new Box(pos).expand(radius, 0.15, radius);

        List<LivingEntity> entities = world.getEntitiesByClass(
                LivingEntity.class,
                triggerBox,
                this::canTriggerTrap
        );

        if (entities.isEmpty()) {
            return;
        }

        startTriggered0(world, pos, config);
    }

    private boolean canTriggerTrap(LivingEntity entity) {
        if (!entity.isAlive()) {
            return false;
        }

        if (entity instanceof PlayerEntity player && player.isSpectator()) {
            return false;
        }

        return true;
    }

    private void startTriggered0(ServerWorld world, BlockPos pos, QuietPlaceConfig.GlassBottleTrap config) {
        this.triggerStage = 1;
        this.triggerTicks = Math.max(1, config.triggered0Ticks);
        this.emittedNoise = false;

        setBlockTriggerStage(world, pos, 1);

        world.playSound(
                null,
                pos,
                ModSounds.GLASS_BOTTLE_TRAP_TRIGGER_0,
                SoundCategory.BLOCKS,
                config.trigger0SoundVolume,
                0.95f + world.getRandom().nextFloat() * 0.1f
        );

        markDirty();
    }

    private void startTriggered1(ServerWorld world, BlockPos pos, QuietPlaceConfig.GlassBottleTrap config) {
        this.triggerStage = 2;
        this.triggerTicks = Math.max(1, config.triggered1Ticks);

        setBlockTriggerStage(world, pos, 2);

        world.playSound(
                null,
                pos,
                ModSounds.GLASS_BOTTLE_TRAP_TRIGGER_1,
                SoundCategory.BLOCKS,
                config.trigger1SoundVolume,
                0.95f + world.getRandom().nextFloat() * 0.1f
        );

        spawnDeathAngelIfConfigured(world, pos, config);

        /*
         * Emit noise after possible spawn, so the spawned Death Angel can immediately
         * hear the trap and start investigating it.
         */
        emitTrapNoise(world, pos, config);

        this.emittedNoise = true;
        markDirty();
    }

    private void tickTriggered(ServerWorld world, BlockPos pos, QuietPlaceConfig.GlassBottleTrap config) {
        if (this.triggerTicks > 0) {
            this.triggerTicks--;
            markDirty();
            return;
        }

        if (this.triggerStage == 1) {
            startTriggered1(world, pos, config);
            return;
        }

        if (this.triggerStage == 2) {
            breakAfterTrigger(world, pos, config);
        }
    }
    private void spawnDeathAngelIfConfigured(
            ServerWorld world,
            BlockPos trapPos,
            QuietPlaceConfig.GlassBottleTrap config
    ) {
        if (!config.spawnDeathAngelOnTrigger) {
            return;
        }

        if (world.getRandom().nextFloat() > config.spawnDeathAngelChance) {
            return;
        }

        if (countNearbyDeathAngels(world, trapPos, config.nearbyDeathAngelCheckRadius) >= config.maxSpawnedDeathAngelsNearby) {
            return;
        }

        BlockPos spawnPos = findDeathAngelSpawnPos(world, trapPos, config);

        if (spawnPos == null) {
            return;
        }

        DeathAngelEntity deathAngel = ModEntities.DEATH_ANGEL.create(world);

        if (deathAngel == null) {
            return;
        }

        deathAngel.refreshPositionAndAngles(
                spawnPos.getX() + 0.5,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5,
                world.getRandom().nextFloat() * 360.0f,
                0.0f
        );

        world.spawnEntity(deathAngel);
    }

    private int countNearbyDeathAngels(ServerWorld world, BlockPos centerPos, double radius) {
        Box searchBox = new Box(centerPos).expand(radius);

        return world.getEntitiesByClass(
                DeathAngelEntity.class,
                searchBox,
                DeathAngelEntity::isAlive
        ).size();
    }

    private BlockPos findDeathAngelSpawnPos(
            ServerWorld world,
            BlockPos trapPos,
            QuietPlaceConfig.GlassBottleTrap config
    ) {
        Random random = world.getRandom();

        int minDistance = Math.max(4, config.spawnDeathAngelMinDistance);
        int maxDistance = Math.max(minDistance, config.spawnDeathAngelMaxDistance);

        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            int distance = minDistance + random.nextInt(maxDistance - minDistance + 1);

            int x = trapPos.getX() + (int) Math.round(Math.cos(angle) * distance);
            int z = trapPos.getZ() + (int) Math.round(Math.sin(angle) * distance);

            BlockPos surfacePos = world.getTopPosition(
                    Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                    new BlockPos(x, trapPos.getY(), z)
            );

            if (!world.getWorldBorder().contains(surfacePos)) {
                continue;
            }

            if (!world.isAir(surfacePos)) {
                continue;
            }

            if (!world.isAir(surfacePos.up())) {
                continue;
            }

            return surfacePos;
        }

        return null;
    }

    private void emitTrapNoise(ServerWorld world, BlockPos pos, QuietPlaceConfig.GlassBottleTrap config) {
        NoiseSystem.emitNoise(new NoiseEvent(
                world,
                pos.toCenterPos(),
                config.noiseStrength,
                config.noiseRadius,
                null,
                NoiseType.GLASS_BOTTLE_TRAP,
                true,
                true,
                world.getTime()
        ));
    }

    private void breakAfterTrigger(ServerWorld world, BlockPos pos, QuietPlaceConfig.GlassBottleTrap config) {
        world.spawnParticles(
                ParticleTypes.POOF,
                pos.getX() + 0.5,
                pos.getY() + 0.45,
                pos.getZ() + 0.5,
                config.breakParticleCount,
                0.25,
                0.25,
                0.25,
                0.02
        );

        world.removeBlock(pos, false);
    }

    private void setBlockTriggerStage(ServerWorld world, BlockPos pos, int stage) {
        BlockState state = world.getBlockState(pos);

        if (!state.contains(GlassBottleTrapBlock.TRIGGER_STAGE)) {
            return;
        }

        world.setBlockState(
                pos,
                state.with(GlassBottleTrapBlock.TRIGGER_STAGE, stage),
                3
        );
    }

    private int getStageFromState(BlockState state) {
        if (!state.contains(GlassBottleTrapBlock.TRIGGER_STAGE)) {
            return 0;
        }

        return state.get(GlassBottleTrapBlock.TRIGGER_STAGE);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
                this,
                "glass_bottle_trap_controller",
                2,
                animationState -> {
                    int stage = getStageFromState(this.getCachedState());

                    if (stage == 1) {
                        animationState.setAndContinue(TRIGGERED_0);
                        return PlayState.CONTINUE;
                    }

                    if (stage == 2) {
                        animationState.setAndContinue(TRIGGERED_1);
                        return PlayState.CONTINUE;
                    }

                    if (this.idleSwingTicks > 0) {
                        animationState.setAndContinue(IDLE_SWING);
                        return PlayState.CONTINUE;
                    }

                    return PlayState.STOP;
                }
        ));
    }

    private boolean hasActiveDisarmSession() {
        return this.disarmingPlayerUuid != null;
    }

    public void handleDisarmInteraction(ServerPlayerEntity player) {
        if (!(this.world instanceof ServerWorld)) {
            return;
        }

        if (this.triggerStage != 0) {
            player.sendMessage(Text.literal("The trap is already triggered!"), true);
            return;
        }

        if (hasActiveDisarmSession()) {
            if (player.getUuid().equals(this.disarmingPlayerUuid)) {
                openDisarmScreen(player);
            } else {
                player.sendMessage(Text.literal("Someone is already disarming this trap."), true);
            }

            return;
        }

        this.disarmingPlayerUuid = player.getUuid();
        this.disarmReady = false;
        this.disarmClicks = 0;
        this.disarmTicksRemaining = 0;

        player.sendMessage(Text.literal("Prepare to disarm the trap."), true);
        openDisarmScreen(player);

        markDirty();
    }

    private void openDisarmScreen(ServerPlayerEntity player) {
        ServerPlayNetworking.send(
                player,
                new GlassBottleTrapOpenDisarmPayload(
                        this.pos,
                        DISARM_REQUIRED_CLICKS,
                        DISARM_TIME_LIMIT_TICKS
                )
        );
    }

    public void handleDisarmClick(ServerPlayerEntity player) {
        if (!(this.world instanceof ServerWorld serverWorld)) {
            return;
        }

        if (!hasActiveDisarmSession()) {
            return;
        }

        if (!this.disarmReady) {
            return;
        }

        if (!player.getUuid().equals(this.disarmingPlayerUuid)) {
            return;
        }

        if (player.squaredDistanceTo(this.pos.toCenterPos()) > DISARM_MAX_DISTANCE_SQUARED) {
            failDisarm(serverWorld, this.pos, QuietPlaceConfig.get().glassBottleTrap, player, "You moved too far!");
            return;
        }

        if (this.triggerStage != 0) {
            return;
        }

        this.disarmClicks++;

        if (this.disarmClicks >= DISARM_REQUIRED_CLICKS) {
            disarmSuccessfully(serverWorld, player);
            return;
        }

        player.sendMessage(
                Text.literal("Disarming... " + this.disarmClicks + " / " + DISARM_REQUIRED_CLICKS),
                true
        );

        markDirty();
    }

    private void tickDisarmSession(ServerWorld world, BlockPos pos, QuietPlaceConfig.GlassBottleTrap config) {
        ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(this.disarmingPlayerUuid);

        if (player == null) {
            failDisarm(world, pos, config, null, null);
            return;
        }

        if (player.squaredDistanceTo(pos.toCenterPos()) > DISARM_MAX_DISTANCE_SQUARED) {
            failDisarm(world, pos, config, player, "You moved too far!");
            return;
        }

        if (!this.disarmReady) {
            markDirty();
            return;
        }

        this.disarmTicksRemaining--;

        if (this.disarmTicksRemaining <= 0) {
            failDisarm(world, pos, config, player, "Too slow!");
            return;
        }

        markDirty();
    }

    private void disarmSuccessfully(ServerWorld world, ServerPlayerEntity player) {
        clearDisarmSession();

        ItemStack returnedTrap = new ItemStack(this.getCachedState().getBlock().asItem());

        world.spawnParticles(
                ParticleTypes.POOF,
                this.pos.getX() + 0.5,
                this.pos.getY() + 0.35,
                this.pos.getZ() + 0.5,
                8,
                0.15,
                0.10,
                0.15,
                0.01
        );

        world.playSound(
                null,
                player.getBlockPos(),
                SoundEvents.ENTITY_ITEM_PICKUP,
                SoundCategory.PLAYERS,
                0.7f,
                1.0f
        );

        world.removeBlock(this.pos, false);

        boolean inserted = player.getInventory().insertStack(returnedTrap);

        if (!inserted || !returnedTrap.isEmpty()) {
            player.dropItem(returnedTrap, false);
        }

        player.sendMessage(Text.literal("Trap disarmed."), true);
    }

    private void failDisarm(
            ServerWorld world,
            BlockPos pos,
            QuietPlaceConfig.GlassBottleTrap config,
            ServerPlayerEntity player,
            String message
    ) {
        clearDisarmSession();

        if (player != null && message != null) {
            player.sendMessage(Text.literal(message), true);
        }

        startTriggered0(world, pos, config);
    }

    private void clearDisarmSession() {
        this.disarmingPlayerUuid = null;
        this.disarmReady = false;
        this.disarmClicks = 0;
        this.disarmTicksRemaining = 0;

        markDirty();
    }
    public void handleDisarmReady(ServerPlayerEntity player) {
        if (!(this.world instanceof ServerWorld serverWorld)) {
            return;
        }

        if (!hasActiveDisarmSession()) {
            return;
        }

        if (!player.getUuid().equals(this.disarmingPlayerUuid)) {
            return;
        }

        if (this.triggerStage != 0) {
            return;
        }

        if (player.squaredDistanceTo(this.pos.toCenterPos()) > DISARM_MAX_DISTANCE_SQUARED) {
            failDisarm(serverWorld, this.pos, QuietPlaceConfig.get().glassBottleTrap, player, "You moved too far!");
            return;
        }

        this.disarmReady = true;
        this.disarmTicksRemaining = DISARM_TIME_LIMIT_TICKS;

        player.sendMessage(Text.literal("Disarm started!"), true);

        markDirty();
    }
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);

        nbt.putInt("TriggerStage", this.triggerStage);
        nbt.putInt("TriggerTicks", this.triggerTicks);
        nbt.putBoolean("EmittedNoise", this.emittedNoise);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);

        this.triggerStage = nbt.getInt("TriggerStage");
        this.triggerTicks = nbt.getInt("TriggerTicks");
        this.emittedNoise = nbt.getBoolean("EmittedNoise");

        this.disarmingPlayerUuid = null;
        this.disarmClicks = 0;
        this.disarmReady = false;
        this.disarmTicksRemaining = 0;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animationCache;
    }
}