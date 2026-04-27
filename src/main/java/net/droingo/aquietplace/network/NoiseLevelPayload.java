package net.droingo.aquietplace.network;

import net.droingo.aquietplace.AQuietPlace;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record NoiseLevelPayload(float noiseLevel) implements CustomPayload {
    public static final CustomPayload.Id<NoiseLevelPayload> ID = new CustomPayload.Id<>(
            Identifier.of(AQuietPlace.MOD_ID, "noise_level")
    );

    public static final PacketCodec<RegistryByteBuf, NoiseLevelPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.FLOAT,
            NoiseLevelPayload::noiseLevel,
            NoiseLevelPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}