package net.droingo.aquietplace.network;

import net.droingo.aquietplace.AQuietPlace;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class ModNetworking {
    private ModNetworking() {
    }

    public static void registerPayloads() {
        PayloadTypeRegistry.playS2C().register(NoiseLevelPayload.ID, NoiseLevelPayload.CODEC);

        AQuietPlace.LOGGER.info("Registered networking payloads for {}", AQuietPlace.MOD_ID);
    }
}