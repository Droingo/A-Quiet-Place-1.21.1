package net.droingo.aquietplace.client.render.block;

import net.droingo.aquietplace.AQuietPlace;
import net.droingo.aquietplace.block.entity.GlassBottleTrapBlockEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class GlassBottleTrapModel extends GeoModel<GlassBottleTrapBlockEntity> {
    @Override
    public Identifier getModelResource(GlassBottleTrapBlockEntity animatable) {
        return Identifier.of(AQuietPlace.MOD_ID, "geo/block/glass_bottle_trap.geo.json");
    }

    @Override
    public Identifier getTextureResource(GlassBottleTrapBlockEntity animatable) {
        return Identifier.of(AQuietPlace.MOD_ID, "textures/block/glass_bottle_trap.png");
    }

    @Override
    public Identifier getAnimationResource(GlassBottleTrapBlockEntity animatable) {
        return Identifier.of(AQuietPlace.MOD_ID, "animations/block/glass_bottle_trap.animation.json");
    }
}