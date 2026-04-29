package net.droingo.aquietplace.client.network;

import net.droingo.aquietplace.client.hud.ClientSoundMeterData;
import net.droingo.aquietplace.network.SoundMeterScanPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ClientSoundMeterNetworking {
    private ClientSoundMeterNetworking() {
    }

    public static void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(
                SoundMeterScanPayload.ID,
                (payload, context) -> context.client().execute(() ->
                        ClientSoundMeterData.show(
                                payload.ambientLevelOrdinal(),
                                payload.ambientStrengthPercent(),
                                payload.maskingPercent(),
                                payload.sourceFlags(),
                                payload.displayTicks()
                        )
                )
        );
    }
}