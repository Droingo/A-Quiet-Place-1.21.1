package net.droingo.aquietplace.network;

import net.droingo.aquietplace.AQuietPlace;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record NoisemakerSaveSettingsPayload(
        BlockPos pos,
        int delaySeconds,
        float radius,
        float strength,
        boolean armed
) implements CustomPayload {
    public static final Id<NoisemakerSaveSettingsPayload> ID = new Id<>(
            Identifier.of(AQuietPlace.MOD_ID, "save_noisemaker_settings")
    );

    public static final PacketCodec<RegistryByteBuf, NoisemakerSaveSettingsPayload> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC,
            NoisemakerSaveSettingsPayload::pos,
            PacketCodecs.INTEGER,
            NoisemakerSaveSettingsPayload::delaySeconds,
            PacketCodecs.FLOAT,
            NoisemakerSaveSettingsPayload::radius,
            PacketCodecs.FLOAT,
            NoisemakerSaveSettingsPayload::strength,
            PacketCodecs.BOOL,
            NoisemakerSaveSettingsPayload::armed,
            NoisemakerSaveSettingsPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}