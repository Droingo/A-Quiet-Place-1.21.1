package net.droingo.aquietplace;

import net.droingo.aquietplace.client.hud.NoiseHudOverlay;
import net.droingo.aquietplace.client.network.ClientNoiseNetworking;
import net.droingo.aquietplace.client.render.entity.DeathAngelRenderer;
import net.droingo.aquietplace.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class AQuietPlaceClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.DEATH_ANGEL, DeathAngelRenderer::new);

        ClientNoiseNetworking.registerReceivers();
        NoiseHudOverlay.register();
    }
}