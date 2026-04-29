package net.droingo.aquietplace.network;

import net.droingo.aquietplace.AQuietPlace;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SoundMeterScanPayload(
        int ambientLevelOrdinal,
        int ambientStrengthPercent,
        int maskingPercent,
        int sourceFlags,
        int displayTicks
) implements CustomPayload {
    public static final int SOURCE_WATER = 1;
    public static final int SOURCE_LAVA = 1 << 1;
    public static final int SOURCE_WEATHER = 1 << 2;
    public static final int SOURCE_WATERFALL = 1 << 3;

    public static final CustomPayload.Id<SoundMeterScanPayload> ID = new CustomPayload.Id<>(
            Identifier.of(AQuietPlace.MOD_ID, "sound_meter_scan")
    );

    public static final PacketCodec<RegistryByteBuf, SoundMeterScanPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER,
            SoundMeterScanPayload::ambientLevelOrdinal,
            PacketCodecs.INTEGER,
            SoundMeterScanPayload::ambientStrengthPercent,
            PacketCodecs.INTEGER,
            SoundMeterScanPayload::maskingPercent,
            PacketCodecs.INTEGER,
            SoundMeterScanPayload::sourceFlags,
            PacketCodecs.INTEGER,
            SoundMeterScanPayload::displayTicks,
            SoundMeterScanPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}