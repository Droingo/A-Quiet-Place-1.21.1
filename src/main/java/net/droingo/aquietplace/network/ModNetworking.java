package net.droingo.aquietplace.network;

import net.droingo.aquietplace.AQuietPlace;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.droingo.aquietplace.block.entity.NoisemakerBlockEntity;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.droingo.aquietplace.block.entity.GlassBottleTrapBlockEntity;

public final class ModNetworking {
    private ModNetworking() {
    }


    public static void registerPayloads() {
        PayloadTypeRegistry.playS2C().register(NoiseLevelPayload.ID, NoiseLevelPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NoisemakerOpenScreenPayload.ID, NoisemakerOpenScreenPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NoisemakerSaveSettingsPayload.ID, NoisemakerSaveSettingsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                GlassBottleTrapOpenDisarmPayload.ID,
                GlassBottleTrapOpenDisarmPayload.CODEC
        );

        PayloadTypeRegistry.playC2S().register(
                GlassBottleTrapDisarmClickPayload.ID,
                GlassBottleTrapDisarmClickPayload.CODEC
        );
        PayloadTypeRegistry.playC2S().register(
                GlassBottleTrapDisarmReadyPayload.ID,
                GlassBottleTrapDisarmReadyPayload.CODEC
        );

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
        ServerPlayNetworking.registerGlobalReceiver(
                GlassBottleTrapDisarmReadyPayload.ID,
                (payload, context) -> context.server().execute(() -> {
                    if (context.player().getWorld().getBlockEntity(payload.pos()) instanceof GlassBottleTrapBlockEntity trap) {
                        trap.handleDisarmReady(context.player());
                    }
                })
        );

        ServerPlayNetworking.registerGlobalReceiver(
                GlassBottleTrapDisarmClickPayload.ID,
                (payload, context) -> context.server().execute(() -> {
                    if (context.player().getWorld().getBlockEntity(payload.pos()) instanceof GlassBottleTrapBlockEntity trap) {
                        trap.handleDisarmClick(context.player());
                    }
                })
        );


        AQuietPlace.LOGGER.info("Registered networking payloads for {}", AQuietPlace.MOD_ID);
    }
}