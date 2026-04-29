package net.droingo.aquietplace.network;

import net.droingo.aquietplace.AQuietPlace;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record GlassBottleTrapOpenDisarmPayload(
        BlockPos pos,
        int requiredClicks,
        int timeLimitTicks
) implements CustomPayload {
    public static final CustomPayload.Id<GlassBottleTrapOpenDisarmPayload> ID = new CustomPayload.Id<>(
            Identifier.of(AQuietPlace.MOD_ID, "open_glass_bottle_trap_disarm")
    );

    public static final PacketCodec<RegistryByteBuf, GlassBottleTrapOpenDisarmPayload> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC,
            GlassBottleTrapOpenDisarmPayload::pos,
            PacketCodecs.INTEGER,
            GlassBottleTrapOpenDisarmPayload::requiredClicks,
            PacketCodecs.INTEGER,
            GlassBottleTrapOpenDisarmPayload::timeLimitTicks,
            GlassBottleTrapOpenDisarmPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}