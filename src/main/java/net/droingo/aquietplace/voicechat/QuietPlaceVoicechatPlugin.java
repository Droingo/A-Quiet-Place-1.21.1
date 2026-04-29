package net.droingo.aquietplace.voicechat;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.PlayerDisconnectedEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import net.droingo.aquietplace.AQuietPlace;
import net.droingo.aquietplace.config.QuietPlaceConfig;
import net.droingo.aquietplace.noise.NoiseEvent;
import net.droingo.aquietplace.noise.NoiseSystem;
import net.droingo.aquietplace.noise.NoiseType;
import net.droingo.aquietplace.noise.PlayerNoiseHandler;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class QuietPlaceVoicechatPlugin implements VoicechatPlugin {
    private VoicechatApi api;

    private final Map<UUID, OpusDecoder> decodersByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> cooldownTicksByPlayer = new ConcurrentHashMap<>();

    @Override
    public String getPluginId() {
        return AQuietPlace.MOD_ID;
    }

    @Override
    public void initialize(VoicechatApi api) {
        this.api = api;
        AQuietPlace.LOGGER.info("Simple Voice Chat API initialized for {}", AQuietPlace.MOD_ID);
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
        registration.registerEvent(PlayerDisconnectedEvent.class, this::onPlayerDisconnected);

        AQuietPlace.LOGGER.info("Registered Simple Voice Chat noise events");
    }

    private void onMicrophonePacket(MicrophonePacketEvent event) {
        QuietPlaceConfig.VoiceChatNoise config = QuietPlaceConfig.get().voiceChatNoise;

        if (!config.enabled) {
            return;
        }

        if (this.api == null) {
            return;
        }

        VoicechatConnection senderConnection = event.getSenderConnection();

        if (senderConnection == null) {
            return;
        }

        Object rawPlayer = senderConnection.getPlayer().getPlayer();

        if (!(rawPlayer instanceof ServerPlayerEntity player)) {
            return;
        }

        if (player.isSpectator()) {
            return;
        }

        UUID playerUuid = player.getUuid();

        if (isCoolingDown(playerUuid)) {
            return;
        }

        byte[] opusData = event.getPacket().getOpusEncodedData();

        if (opusData == null || opusData.length == 0) {
            return;
        }

        short[] decodedAudio = decodeAudio(playerUuid, opusData);

        if (decodedAudio == null || decodedAudio.length == 0) {
            return;
        }

        float rms = calculateRms(decodedAudio);

        if (rms < config.minimumRms) {
            return;
        }

        float strength = clamp(
                rms * config.rmsStrengthMultiplier,
                config.minimumStrength,
                config.maximumStrength
        );

        float radius = config.minimumRadius + strength * (config.maximumRadius - config.minimumRadius);

        if (event.getPacket().isWhispering()) {
            strength *= config.whisperStrengthMultiplier;
            radius *= config.whisperRadiusMultiplier;
        }

        NoiseSystem.emitNoise(NoiseEvent.create(
                player.getServerWorld(),
                player.getPos(),
                strength,
                radius,
                player,
                NoiseType.VOICE,
                true,
                true
        ));

        PlayerNoiseHandler.addHudNoise(player, clamp01(strength * config.hudMultiplier));

        cooldownTicksByPlayer.put(playerUuid, config.packetCooldownTicks);
    }

    private boolean isCoolingDown(UUID playerUuid) {
        Integer cooldownTicks = cooldownTicksByPlayer.get(playerUuid);

        if (cooldownTicks == null || cooldownTicks <= 0) {
            return false;
        }

        cooldownTicksByPlayer.put(playerUuid, cooldownTicks - 1);
        return true;
    }

    private short[] decodeAudio(UUID playerUuid, byte[] opusData) {
        try {
            OpusDecoder decoder = decodersByPlayer.computeIfAbsent(playerUuid, uuid -> this.api.createDecoder());
            return decoder.decode(opusData);
        } catch (Exception exception) {
            AQuietPlace.LOGGER.warn("Failed to decode Simple Voice Chat packet for player {}", playerUuid, exception);

            OpusDecoder brokenDecoder = decodersByPlayer.remove(playerUuid);

            if (brokenDecoder != null && !brokenDecoder.isClosed()) {
                brokenDecoder.close();
            }

            return null;
        }
    }

    private void onPlayerDisconnected(PlayerDisconnectedEvent event) {
        UUID playerUuid = event.getPlayerUuid();

        cooldownTicksByPlayer.remove(playerUuid);

        OpusDecoder decoder = decodersByPlayer.remove(playerUuid);

        if (decoder != null && !decoder.isClosed()) {
            decoder.close();
        }
    }

    private float calculateRms(short[] audio) {
        double sumSquares = 0.0;

        for (short sample : audio) {
            double normalizedSample = sample / 32768.0;
            sumSquares += normalizedSample * normalizedSample;
        }

        return (float) Math.sqrt(sumSquares / audio.length);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private float clamp01(float value) {
        return clamp(value, 0.0f, 1.0f);
    }
}