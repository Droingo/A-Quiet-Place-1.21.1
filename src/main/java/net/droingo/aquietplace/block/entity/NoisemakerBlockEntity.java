package net.droingo.aquietplace.block.entity;

import net.droingo.aquietplace.block.NoisemakerBlock;
import net.droingo.aquietplace.noise.NoiseEvent;
import net.droingo.aquietplace.noise.NoiseSystem;
import net.droingo.aquietplace.noise.NoiseType;
import net.droingo.aquietplace.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class NoisemakerBlockEntity extends BlockEntity {
    private static final int BLINK_INTERVAL_TICKS = 10;

    private int delaySeconds = 5;
    private float radius = 24.0f;
    private float strength = 1.0f;
    private boolean armed = false;

    private boolean active = false;
    private boolean blink = false;
    private int countdownTicks = 0;
    private int blinkTicks = 0;

    public NoisemakerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NOISEMAKER, pos, state);
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

    public void setSettings(int delaySeconds, float radius, float strength, boolean armed) {
        this.delaySeconds = clamp(delaySeconds, 0, 60);
        this.radius = clamp(radius, 1.0f, 128.0f);
        this.strength = clamp(strength, 0.05f, 1.0f);

        if (armed) {
            startCountdown();
        } else {
            disarm();
        }

        this.markDirty();
        syncBlockState();
    }

    private void tickServer(ServerWorld world, BlockPos pos, BlockState state) {
        if (!this.armed || !this.active) {
            return;
        }

        tickBlink();

        if (this.countdownTicks > 0) {
            this.countdownTicks--;
            markDirty();
            return;
        }

        emitNoise(world, pos);
        disarm();
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

    private void startCountdown() {
        this.armed = true;
        this.active = true;
        this.blink = false;
        this.blinkTicks = BLINK_INTERVAL_TICKS;
        this.countdownTicks = this.delaySeconds * 20;
    }

    private void disarm() {
        this.armed = false;
        this.active = false;
        this.blink = false;
        this.countdownTicks = 0;
        this.blinkTicks = 0;

        syncBlockState();
    }

    private void emitNoise(ServerWorld world, BlockPos pos) {
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
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);

        this.delaySeconds = nbt.getInt("DelaySeconds");
        this.radius = nbt.getFloat("Radius");
        this.strength = nbt.getFloat("Strength");
        this.armed = nbt.getBoolean("Armed");
        this.active = nbt.getBoolean("Active");
        this.blink = nbt.getBoolean("Blink");
        this.countdownTicks = nbt.getInt("CountdownTicks");
        this.blinkTicks = nbt.getInt("BlinkTicks");
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}