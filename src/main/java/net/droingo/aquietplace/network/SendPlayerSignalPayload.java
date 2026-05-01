package net.droingo.aquietplace.network;

import net.droingo.aquietplace.AQuietPlace;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SendPlayerSignalPayload(
        int signalTypeId,
        int colorRgb
) implements CustomPayload {
    public static final CustomPayload.Id<SendPlayerSignalPayload> ID = new CustomPayload.Id<>(
            Identifier.of(AQuietPlace.MOD_ID, "send_player_signal")
    );

    public static final PacketCodec<RegistryByteBuf, SendPlayerSignalPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER,
            SendPlayerSignalPayload::signalTypeId,
            PacketCodecs.INTEGER,
            SendPlayerSignalPayload::colorRgb,
            SendPlayerSignalPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}