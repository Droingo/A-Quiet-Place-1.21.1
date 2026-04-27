package net.droingo.aquietplace.client.render.entity;

import net.droingo.aquietplace.entity.deathangel.DeathAngelEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DeathAngelRenderer extends GeoEntityRenderer<DeathAngelEntity> {
    public DeathAngelRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new DeathAngelModel());
        this.shadowRadius = 0.9f;
    }
}