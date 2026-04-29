package net.droingo.aquietplace;

import net.droingo.aquietplace.client.hud.NoiseHudOverlay;
import net.droingo.aquietplace.client.network.ClientNoiseNetworking;
import net.droingo.aquietplace.client.render.entity.DeathAngelRenderer;
import net.droingo.aquietplace.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.droingo.aquietplace.registry.ModBlocks;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;
import net.droingo.aquietplace.client.network.ClientNoisemakerNetworking;
import net.droingo.aquietplace.client.hud.ClientItemTooltips;
import net.droingo.aquietplace.client.render.block.GlassBottleTrapRenderer;
import net.droingo.aquietplace.registry.ModBlockEntities;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public class AQuietPlaceClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.DEATH_ANGEL, DeathAngelRenderer::new);

        ClientNoiseNetworking.registerReceivers();
        NoiseHudOverlay.register();
        ClientNoisemakerNetworking.register();
        ClientItemTooltips.register();

        BlockRenderLayerMap.INSTANCE.putBlock(
                ModBlocks.NEWSPAPER_SOUNDPROOFING,
                RenderLayer.getCutout()
        );
        BlockEntityRendererFactories.register(
                ModBlockEntities.GLASS_BOTTLE_TRAP,
                GlassBottleTrapRenderer::new
        );

    }

}