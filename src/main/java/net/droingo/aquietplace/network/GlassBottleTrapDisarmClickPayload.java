package net.droingo.aquietplace.network;

import net.droingo.aquietplace.AQuietPlace;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record GlassBottleTrapDisarmClickPayload(
        BlockPos pos
) implements CustomPayload {
    public static final CustomPayload.Id<GlassBottleTrapDisarmClickPayload> ID = new CustomPayload.Id<>(
            Identifier.of(AQuietPlace.MOD_ID, "glass_bottle_trap_disarm_click")
    );

    public static final PacketCodec<RegistryByteBuf, GlassBottleTrapDisarmClickPayload> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC,
            GlassBottleTrapDisarmClickPayload::pos,
            GlassBottleTrapDisarmClickPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}