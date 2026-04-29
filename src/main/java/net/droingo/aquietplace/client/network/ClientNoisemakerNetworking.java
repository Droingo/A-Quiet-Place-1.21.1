package net.droingo.aquietplace.client.network;

import net.droingo.aquietplace.client.screen.NoisemakerScreen;
import net.droingo.aquietplace.network.NoisemakerOpenScreenPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ClientNoisemakerNetworking {
    private ClientNoisemakerNetworking() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(NoisemakerOpenScreenPayload.ID, (payload, context) ->
                context.client().execute(() ->
                        context.client().setScreen(new NoisemakerScreen(payload))
                )
        );
    }
}