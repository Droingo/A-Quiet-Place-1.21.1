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
        int flags,
        int countdownTicks
) implements CustomPayload {
    private static final int ARMED_FLAG = 1;
    private static final int ACTIVE_FLAG = 2;

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
            PacketCodecs.INTEGER,
            NoisemakerOpenScreenPayload::flags,
            PacketCodecs.INTEGER,
            NoisemakerOpenScreenPayload::countdownTicks,
            NoisemakerOpenScreenPayload::new
    );

    public static NoisemakerOpenScreenPayload create(
            BlockPos pos,
            int delaySeconds,
            float radius,
            float strength,
            boolean armed,
            boolean active,
            int countdownTicks
    ) {
        int flags = 0;

        if (armed) {
            flags |= ARMED_FLAG;
        }

        if (active) {
            flags |= ACTIVE_FLAG;
        }

        return new NoisemakerOpenScreenPayload(
                pos,
                delaySeconds,
                radius,
                strength,
                flags,
                countdownTicks
        );
    }

    public boolean armed() {
        return (this.flags & ARMED_FLAG) != 0;
    }

    public boolean active() {
        return (this.flags & ACTIVE_FLAG) != 0;
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}