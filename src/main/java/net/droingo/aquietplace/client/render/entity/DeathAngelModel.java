package net.droingo.aquietplace.client.render.entity;

import net.droingo.aquietplace.AQuietPlace;
import net.droingo.aquietplace.entity.deathangel.DeathAngelEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class DeathAngelModel extends GeoModel<DeathAngelEntity> {
    @Override
    public Identifier getModelResource(DeathAngelEntity animatable) {
        return Identifier.of(AQuietPlace.MOD_ID, "geo/entity/death_angel.geo.json");
    }

    @Override
    public Identifier getTextureResource(DeathAngelEntity animatable) {
        return Identifier.of(AQuietPlace.MOD_ID, "textures/entity/death_angel.png");
    }

    @Override
    public Identifier getAnimationResource(DeathAngelEntity animatable) {
        return Identifier.of(AQuietPlace.MOD_ID, "animations/entity/death_angel.animation.json");
    }
}