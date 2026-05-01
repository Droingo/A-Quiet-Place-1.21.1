package net.droingo.aquietplace.entity.signal;

import net.droingo.aquietplace.signal.SignalType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.UUID;

public class PlayerSignalEntity extends Entity {
    private static final TrackedData<Integer> SIGNAL_TYPE_ID = DataTracker.registerData(
            PlayerSignalEntity.class,
            TrackedDataHandlerRegistry.INTEGER
    );

    private static final TrackedData<Integer> SIGNAL_COLOR_RGB = DataTracker.registerData(
            PlayerSignalEntity.class,
            TrackedDataHandlerRegistry.INTEGER
    );

    private static final int DEFAULT_DURATION_TICKS = 60;

    private UUID ownerUuid;
    private int durationTicks = DEFAULT_DURATION_TICKS;

    public PlayerSignalEntity(EntityType<? extends PlayerSignalEntity> entityType, World world) {
        super(entityType, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public static PlayerSignalEntity create(
            EntityType<? extends PlayerSignalEntity> entityType,
            ServerWorld world,
            Entity owner,
            SignalType signalType,
            int colorRgb
    ){
        PlayerSignalEntity signalEntity = new PlayerSignalEntity(entityType, world);
        signalEntity.setOwner(owner);
        signalEntity.setSignalType(signalType);
        signalEntity.setColorRgb(colorRgb);
        signalEntity.refreshPositionNearOwner(owner);
        return signalEntity;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(SIGNAL_TYPE_ID, SignalType.MOVE.toNetworkId());
        builder.add(SIGNAL_COLOR_RGB, 0xF1C27D);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.getWorld().isClient()) {
            return;
        }

        this.durationTicks--;

        if (this.durationTicks <= 0) {
            this.discard();
            return;
        }

        Entity owner = getOwnerEntity();

        if (owner == null || !owner.isAlive()) {
            this.discard();
            return;
        }

        refreshPositionNearOwner(owner);
        this.setVelocity(Vec3d.ZERO);
    }

    public void refresh(Entity owner, SignalType signalType, int colorRgb) {
        setOwner(owner);
        setSignalType(signalType);
        setColorRgb(colorRgb);
        this.durationTicks = DEFAULT_DURATION_TICKS;
        refreshPositionNearOwner(owner);
    }

    private void refreshPositionNearOwner(Entity owner) {
        this.refreshPositionAndAngles(
                owner.getX(),
                owner.getY() + owner.getHeight() + 0.85,
                owner.getZ(),
                owner.getYaw(),
                0.0f
        );
    }

    public void setColorRgb(int colorRgb) {
        this.dataTracker.set(SIGNAL_COLOR_RGB, colorRgb & 0xFFFFFF);
    }

    public int getColorRgb() {
        return this.dataTracker.get(SIGNAL_COLOR_RGB) & 0xFFFFFF;
    }

    public void setOwner(Entity owner) {
        this.ownerUuid = owner.getUuid();
    }

    public UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    private Entity getOwnerEntity() {
        if (this.ownerUuid == null) {
            return null;
        }

        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return null;
        }

        return serverWorld.getEntity(this.ownerUuid);
    }

    public void setSignalType(SignalType signalType) {
        this.dataTracker.set(SIGNAL_TYPE_ID, signalType.toNetworkId());
    }

    public SignalType getSignalType() {
        return SignalType.fromNetworkId(this.dataTracker.get(SIGNAL_TYPE_ID));
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("ColorRgb")) {
            this.setColorRgb(nbt.getInt("ColorRgb"));
        }

        if (nbt.contains("OwnerUuid")) {
            try {
                this.ownerUuid = UUID.fromString(nbt.getString("OwnerUuid"));
            } catch (IllegalArgumentException ignored) {
                this.ownerUuid = null;
            }
        }

        this.durationTicks = nbt.getInt("DurationTicks");

        if (this.durationTicks <= 0) {
            this.durationTicks = DEFAULT_DURATION_TICKS;
        }

        this.setSignalType(SignalType.fromNetworkId(nbt.getInt("SignalType")));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        if (this.ownerUuid != null) {
            nbt.putString("OwnerUuid", this.ownerUuid.toString());
        }

        nbt.putInt("DurationTicks", this.durationTicks);
        nbt.putInt("SignalType", this.getSignalType().toNetworkId());
        nbt.putInt("ColorRgb", this.getColorRgb());
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket(EntityTrackerEntry entityTrackerEntry) {
        return new EntitySpawnS2CPacket(this, entityTrackerEntry);
    }
}