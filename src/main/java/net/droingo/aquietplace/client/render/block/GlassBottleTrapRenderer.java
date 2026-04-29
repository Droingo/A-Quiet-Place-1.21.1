package net.droingo.aquietplace.client.render.block;

import net.droingo.aquietplace.block.entity.GlassBottleTrapBlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class GlassBottleTrapRenderer extends GeoBlockRenderer<GlassBottleTrapBlockEntity> {
    public GlassBottleTrapRenderer(BlockEntityRendererFactory.Context context) {
        super(new GlassBottleTrapModel());
    }
}