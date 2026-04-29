package net.droingo.aquietplace.network;

import net.droingo.aquietplace.AQuietPlace;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.droingo.aquietplace.block.entity.NoisemakerBlockEntity;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class ModNetworking {
    private ModNetworking() {
    }


    public static void registerPayloads() {
        PayloadTypeRegistry.playS2C().register(NoiseLevelPayload.ID, NoiseLevelPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NoisemakerOpenScreenPayload.ID, NoisemakerOpenScreenPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NoisemakerSaveSettingsPayload.ID, NoisemakerSaveSettingsPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(NoisemakerSaveSettingsPayload.ID, (payload, context) ->
                context.server().execute(() -> {
                    if (context.player().getWorld().getBlockEntity(payload.pos()) instanceof NoisemakerBlockEntity noisemaker) {
                        noisemaker.setSettings(
                                payload.delaySeconds(),
                                payload.radius(),
                                payload.strength(),
                                payload.armed()
                        );

                        context.player().sendMessage(
                                net.minecraft.text.Text.literal("Saved noisemaker settings"),
                                true
                        );
                    }
                })
        );

        AQuietPlace.LOGGER.info("Registered networking payloads for {}", AQuietPlace.MOD_ID);
    }
}