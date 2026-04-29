package net.droingo.aquietplace.network;

import net.droingo.aquietplace.AQuietPlace;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record NoisemakerOpenScreenPayload(
        BlockPos pos,
        int delaySeconds,
        float radius,
        float strength,
        boolean armed
) implements CustomPayload {
    public static final Id<NoisemakerOpenScreenPayload> ID = new Id<>(
            Identifier.of(AQuietPlace.MOD_ID, "open_noisemaker_screen")
    );

    public static final PacketCodec<RegistryByteBuf, NoisemakerOpenScreenPayload> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC,
            NoisemakerOpenScreenPayload::pos,
            PacketCodecs.INTEGER,
            NoisemakerOpenScreenPayload::delaySeconds,
            PacketCodecs.FLOAT,
            NoisemakerOpenScreenPayload::radius,
            PacketCodecs.FLOAT,
            NoisemakerOpenScreenPayload::strength,
            PacketCodecs.BOOL,
            NoisemakerOpenScreenPayload::armed,
            NoisemakerOpenScreenPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}