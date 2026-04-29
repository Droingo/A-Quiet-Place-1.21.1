package net.droingo.aquietplace.network;

import net.droingo.aquietplace.AQuietPlace;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record GlassBottleTrapDisarmReadyPayload(
        BlockPos pos
) implements CustomPayload {
    public static final CustomPayload.Id<GlassBottleTrapDisarmReadyPayload> ID = new CustomPayload.Id<>(
            Identifier.of(AQuietPlace.MOD_ID, "glass_bottle_trap_disarm_ready")
    );

    public static final PacketCodec<RegistryByteBuf, GlassBottleTrapDisarmReadyPayload> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC,
            GlassBottleTrapDisarmReadyPayload::pos,
            GlassBottleTrapDisarmReadyPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}