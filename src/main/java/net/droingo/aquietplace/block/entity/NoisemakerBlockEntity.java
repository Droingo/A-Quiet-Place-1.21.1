package net.droingo.aquietplace.block.entity;

import net.droingo.aquietplace.block.NoisemakerBlock;
import net.droingo.aquietplace.config.QuietPlaceConfig;
import net.droingo.aquietplace.noise.NoiseEvent;
import net.droingo.aquietplace.noise.NoiseSystem;
import net.droingo.aquietplace.noise.NoiseType;
import net.droingo.aquietplace.registry.ModBlockEntities;
import net.droingo.aquietplace.registry.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.droingo.aquietplace.registry.ModItems;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;

public class NoisemakerBlockEntity extends BlockEntity {
    private static final int BLINK_INTERVAL_TICKS = 10;

    private static final String ITEM_DATA_KEY = "NoisemakerSettings";
    private static final String DELAY_SECONDS_KEY = "DelaySeconds";
    private static final String RADIUS_KEY = "Radius";
    private static final String STRENGTH_KEY = "Strength";

    private int delaySeconds;
    private float radius;
    private float strength;
    private boolean armed = false;

    private boolean active = false;
    private boolean blink = false;

    private int countdownTicks = 0;
    private int blinkTicks = 0;

    private boolean outputPlaying = false;
    private int outputTicksRemaining = 0;
    private int outputPulseIntervalTicks = 45;
    private int outputPulseCooldownTicks = 0;
    private int outputPulsesRemaining = 0;

    public NoisemakerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NOISEMAKER, pos, state);

        QuietPlaceConfig.Noisemaker config = QuietPlaceConfig.get().noisemaker;
        this.delaySeconds = config.defaultDelaySeconds;
        this.radius = config.defaultRadius;
        this.strength = config.defaultStrength;
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, NoisemakerBlockEntity blockEntity) {
        blockEntity.tickServer(world, pos, state);
    }

    public int getDelaySeconds() {
        return this.delaySeconds;
    }

    public float getRadius() {
        return this.radius;
    }

    public float getStrength() {
        return this.strength;
    }

    public boolean isArmed() {
        return this.armed;
    }

    public boolean isActive() {
        return this.active;
    }

    public int getCountdownTicks() {
        if (this.countdownTicks > 0) {
            return this.countdownTicks;
        }

        return this.outputTicksRemaining;
    }

    public void setSettings(int delaySeconds, float radius, float strength, boolean armed) {
        QuietPlaceConfig.Noisemaker config = QuietPlaceConfig.get().noisemaker;

        this.delaySeconds = clamp(delaySeconds, config.minDelaySeconds, config.maxDelaySeconds);
        this.radius = clamp(radius, config.minRadius, config.maxRadius);
        this.strength = clamp(strength, config.minStrength, config.maxStrength);

        if (armed) {
            startCountdown();
        } else {
            disarm();
        }

        this.markDirty();
        syncBlockState();
    }

    public void startCountdown() {
        if (this.active) {
            return;
        }

        this.armed = true;
        this.active = true;
        this.outputPlaying = false;

        this.blink = false;
        this.blinkTicks = BLINK_INTERVAL_TICKS;
        this.countdownTicks = Math.max(0, this.delaySeconds * 20);

        if (this.world instanceof ServerWorld serverWorld) {
            playStartSound(serverWorld, this.pos);
        }

        this.markDirty();
        syncBlockState();
    }

    public void startCountdownFromRedstone() {
        if (!QuietPlaceConfig.get().noisemaker.redstoneStartsCountdown) {
            return;
        }

        startCountdown();
    }

    public void disarm() {
        this.armed = false;
        this.active = false;
        this.blink = false;

        this.countdownTicks = 0;
        this.blinkTicks = 0;

        this.outputPlaying = false;
        this.outputTicksRemaining = 0;
        this.outputPulseCooldownTicks = 0;
        this.outputPulsesRemaining = 0;

        this.markDirty();
        syncBlockState();
    }

    private void tickServer(ServerWorld world, BlockPos pos, BlockState state) {
        if (!this.armed || !this.active) {
            return;
        }

        tickBlink();

        if (this.outputPlaying) {
            tickOutputPhase(world, pos);
            return;
        }

        if (this.countdownTicks > 0) {
            playCountdownBeepIfNeeded(world, pos);

            this.countdownTicks--;
            markDirty();
            return;
        }

        startOutputPhase(world, pos);
    }

    private void tickBlink() {
        this.blinkTicks--;

        if (this.blinkTicks > 0) {
            return;
        }

        this.blinkTicks = BLINK_INTERVAL_TICKS;
        this.blink = !this.blink;

        syncBlockState();
    }

    private void startOutputPhase(ServerWorld world, BlockPos pos) {
        QuietPlaceConfig.Noisemaker config = QuietPlaceConfig.get().noisemaker;

        int outputDurationTicks = Math.max(1, config.outputDurationTicks);
        int pulseCount = Math.max(1, config.outputNoisePulses);

        this.outputPlaying = true;
        this.outputTicksRemaining = outputDurationTicks;
        this.outputPulsesRemaining = pulseCount;
        this.outputPulseIntervalTicks = Math.max(1, outputDurationTicks / pulseCount);
        this.outputPulseCooldownTicks = 0;

        playOutputSound(world, pos);
        emitNoisePulse(world, pos);

        this.outputPulsesRemaining--;
        this.outputPulseCooldownTicks = this.outputPulseIntervalTicks;

        this.markDirty();
        syncBlockState();
    }

    private void tickOutputPhase(ServerWorld world, BlockPos pos) {
        if (this.outputTicksRemaining > 0) {
            this.outputTicksRemaining--;
        }

        if (this.outputPulsesRemaining > 0) {
            this.outputPulseCooldownTicks--;

            if (this.outputPulseCooldownTicks <= 0) {
                emitNoisePulse(world, pos);
                this.outputPulsesRemaining--;
                this.outputPulseCooldownTicks = this.outputPulseIntervalTicks;
            }
        }

        markDirty();

        if (this.outputTicksRemaining <= 0) {
            finishAfterUse(world, pos);
        }
    }

    private void finishAfterUse(ServerWorld world, BlockPos pos) {
        QuietPlaceConfig.Noisemaker config = QuietPlaceConfig.get().noisemaker;

        if (!config.breaksAfterUse) {
            disarm();
            return;
        }

        spawnBreakParticles(world, pos);
        dropSalvage(world, pos);

        /*
         * Remove the block without dropping the noisemaker itself.
         * Salvage drops are handled manually above.
         */
        world.removeBlock(pos, false);
    }

    private void spawnBreakParticles(ServerWorld world, BlockPos pos) {
        QuietPlaceConfig.Noisemaker config = QuietPlaceConfig.get().noisemaker;

        world.spawnParticles(
                ParticleTypes.POOF,
                pos.getX() + 0.5,
                pos.getY() + 0.35,
                pos.getZ() + 0.5,
                config.breakParticleCount,
                0.28,
                0.18,
                0.28,
                0.025
        );
    }

    private void dropSalvage(ServerWorld world, BlockPos pos) {
        QuietPlaceConfig.Noisemaker config = QuietPlaceConfig.get().noisemaker;

        dropRandomItemCount(
                world,
                pos,
                ModItems.SCRAP_ELECTRONICS,
                config.salvageScrapElectronicsMin,
                config.salvageScrapElectronicsMax
        );

        dropRandomItemCount(
                world,
                pos,
                Items.REDSTONE,
                config.salvageRedstoneMin,
                config.salvageRedstoneMax
        );

        dropRandomItemCount(
                world,
                pos,
                Items.IRON_NUGGET,
                config.salvageIronNuggetsMin,
                config.salvageIronNuggetsMax
        );
    }

    private void dropRandomItemCount(
            ServerWorld world,
            BlockPos pos,
            Item item,
            int min,
            int max
    ) {
        int count = getRandomCount(world, min, max);

        if (count <= 0) {
            return;
        }

        Block.dropStack(
                world,
                pos,
                new ItemStack(item, count)
        );
    }

    private int getRandomCount(ServerWorld world, int min, int max) {
        int safeMin = Math.max(0, min);
        int safeMax = Math.max(safeMin, max);

        if (safeMax <= safeMin) {
            return safeMin;
        }

        return safeMin + world.getRandom().nextInt(safeMax - safeMin + 1);
    }

    private void playStartSound(ServerWorld world, BlockPos pos) {
        QuietPlaceConfig.Noisemaker config = QuietPlaceConfig.get().noisemaker;

        world.playSound(
                null,
                pos,
                ModSounds.NOISEMAKER_START,
                SoundCategory.BLOCKS,
                config.startSoundVolume,
                0.95f + world.getRandom().nextFloat() * 0.1f
        );
    }

    private void playCountdownBeepIfNeeded(ServerWorld world, BlockPos pos) {
        if (this.countdownTicks <= 0) {
            return;
        }

        if (this.countdownTicks % 20 != 0) {
            return;
        }

        QuietPlaceConfig.Noisemaker config = QuietPlaceConfig.get().noisemaker;

        float pitch = this.countdownTicks <= 40 ? 1.35f : 1.0f;

        world.playSound(
                null,
                pos,
                ModSounds.NOISEMAKER_BEEP,
                SoundCategory.BLOCKS,
                config.beepSoundVolume,
                pitch
        );
    }

    private void playOutputSound(ServerWorld world, BlockPos pos) {
        QuietPlaceConfig.Noisemaker config = QuietPlaceConfig.get().noisemaker;

        world.playSound(
                null,
                pos,
                ModSounds.NOISEMAKER_NOISE,
                SoundCategory.BLOCKS,
                config.outputSoundVolume,
                0.95f + world.getRandom().nextFloat() * 0.1f
        );
    }

    private void emitNoisePulse(ServerWorld world, BlockPos pos) {
        NoiseSystem.emitNoise(new NoiseEvent(
                world,
                pos.toCenterPos(),
                this.strength,
                this.radius,
                null,
                NoiseType.NOISEMAKER,
                true,
                true,
                world.getTime()
        ));
    }

    private void syncBlockState() {
        if (this.world == null) {
            return;
        }

        BlockState state = this.getCachedState();

        if (!state.contains(NoisemakerBlock.ARMED)
                || !state.contains(NoisemakerBlock.ACTIVE)
                || !state.contains(NoisemakerBlock.BLINK)) {
            return;
        }

        BlockState updatedState = state
                .with(NoisemakerBlock.ARMED, this.armed)
                .with(NoisemakerBlock.ACTIVE, this.active)
                .with(NoisemakerBlock.BLINK, this.blink);

        if (updatedState != state) {
            this.world.setBlockState(this.pos, updatedState, 3);
        }
    }

    public void writeSettingsToStack(ItemStack stack) {
        NbtCompound settingsNbt = new NbtCompound();
        settingsNbt.putInt(DELAY_SECONDS_KEY, this.delaySeconds);
        settingsNbt.putFloat(RADIUS_KEY, this.radius);
        settingsNbt.putFloat(STRENGTH_KEY, this.strength);

        NbtCompound rootNbt = new NbtCompound();
        rootNbt.put(ITEM_DATA_KEY, settingsNbt);

        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(rootNbt));
    }

    public void readSettingsFromStack(ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);

        if (customData == null) {
            return;
        }

        NbtCompound rootNbt = customData.copyNbt();

        if (!rootNbt.contains(ITEM_DATA_KEY)) {
            return;
        }

        NbtCompound settingsNbt = rootNbt.getCompound(ITEM_DATA_KEY);
        QuietPlaceConfig.Noisemaker config = QuietPlaceConfig.get().noisemaker;

        this.delaySeconds = clamp(settingsNbt.getInt(DELAY_SECONDS_KEY), config.minDelaySeconds, config.maxDelaySeconds);
        this.radius = clamp(settingsNbt.getFloat(RADIUS_KEY), config.minRadius, config.maxRadius);
        this.strength = clamp(settingsNbt.getFloat(STRENGTH_KEY), config.minStrength, config.maxStrength);

        this.armed = false;
        this.active = false;
        this.blink = false;
        this.countdownTicks = 0;
        this.blinkTicks = 0;

        this.outputPlaying = false;
        this.outputTicksRemaining = 0;
        this.outputPulseCooldownTicks = 0;
        this.outputPulsesRemaining = 0;

        this.markDirty();
        syncBlockState();
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);

        nbt.putInt("DelaySeconds", this.delaySeconds);
        nbt.putFloat("Radius", this.radius);
        nbt.putFloat("Strength", this.strength);
        nbt.putBoolean("Armed", this.armed);
        nbt.putBoolean("Active", this.active);
        nbt.putBoolean("Blink", this.blink);
        nbt.putInt("CountdownTicks", this.countdownTicks);
        nbt.putInt("BlinkTicks", this.blinkTicks);

        nbt.putBoolean("OutputPlaying", this.outputPlaying);
        nbt.putInt("OutputTicksRemaining", this.outputTicksRemaining);
        nbt.putInt("OutputPulseIntervalTicks", this.outputPulseIntervalTicks);
        nbt.putInt("OutputPulseCooldownTicks", this.outputPulseCooldownTicks);
        nbt.putInt("OutputPulsesRemaining", this.outputPulsesRemaining);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);

        QuietPlaceConfig.Noisemaker config = QuietPlaceConfig.get().noisemaker;

        this.delaySeconds = clamp(nbt.getInt("DelaySeconds"), config.minDelaySeconds, config.maxDelaySeconds);
        this.radius = clamp(nbt.getFloat("Radius"), config.minRadius, config.maxRadius);
        this.strength = clamp(nbt.getFloat("Strength"), config.minStrength, config.maxStrength);

        this.armed = nbt.getBoolean("Armed");
        this.active = nbt.getBoolean("Active");
        this.blink = nbt.getBoolean("Blink");
        this.countdownTicks = nbt.getInt("CountdownTicks");
        this.blinkTicks = nbt.getInt("BlinkTicks");

        this.outputPlaying = nbt.getBoolean("OutputPlaying");
        this.outputTicksRemaining = nbt.getInt("OutputTicksRemaining");
        this.outputPulseIntervalTicks = Math.max(1, nbt.getInt("OutputPulseIntervalTicks"));
        this.outputPulseCooldownTicks = nbt.getInt("OutputPulseCooldownTicks");
        this.outputPulsesRemaining = nbt.getInt("OutputPulsesRemaining");
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}