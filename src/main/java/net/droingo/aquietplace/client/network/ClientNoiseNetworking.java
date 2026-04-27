package net.droingo.aquietplace.client.network;

import net.droingo.aquietplace.client.noise.ClientNoiseData;
import net.droingo.aquietplace.network.NoiseLevelPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ClientNoiseNetworking {
    private ClientNoiseNetworking() {
    }

    public static void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(NoiseLevelPayload.ID, (payload, context) ->
                context.client().execute(() -> ClientNoiseData.setNoiseLevel(payload.noiseLevel()))
        );
    }
}