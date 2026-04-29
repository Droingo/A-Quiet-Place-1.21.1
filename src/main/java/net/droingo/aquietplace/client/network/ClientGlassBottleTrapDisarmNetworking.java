package net.droingo.aquietplace.client.network;

import net.droingo.aquietplace.client.gui.GlassBottleTrapDisarmScreen;
import net.droingo.aquietplace.network.GlassBottleTrapOpenDisarmPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ClientGlassBottleTrapDisarmNetworking {
    private ClientGlassBottleTrapDisarmNetworking() {
    }

    public static void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(
                GlassBottleTrapOpenDisarmPayload.ID,
                (payload, context) -> context.client().execute(() ->
                        context.client().setScreen(new GlassBottleTrapDisarmScreen(
                                payload.pos(),
                                payload.requiredClicks(),
                                payload.timeLimitTicks()
                        ))
                )
        );
    }
}