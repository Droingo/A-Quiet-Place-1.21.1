package net.droingo.aquietplace.client.render.entity;

import com.nyfaria.awcapi.ClientClimberHelper;
import net.droingo.aquietplace.entity.deathangel.DeathAngelEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DeathAngelRenderer extends GeoEntityRenderer<DeathAngelEntity> {
    public DeathAngelRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new DeathAngelModel());
        this.shadowRadius = 0.9f;
    }

    @Override
    public void render(
            DeathAngelEntity entity,
            float entityYaw,
            float partialTick,
            MatrixStack matrixStack,
            VertexConsumerProvider bufferSource,
            int packedLight
    ) {
        ClientClimberHelper.preRenderClimber(entity, partialTick, matrixStack);
        super.render(entity, entityYaw, partialTick, matrixStack, bufferSource, packedLight);
        ClientClimberHelper.postRenderClimber(entity, partialTick, matrixStack, bufferSource);
    }
}