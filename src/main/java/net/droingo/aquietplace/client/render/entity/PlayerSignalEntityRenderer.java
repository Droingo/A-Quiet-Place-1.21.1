package net.droingo.aquietplace.client.render.entity;

import net.droingo.aquietplace.AQuietPlace;
import net.droingo.aquietplace.entity.signal.PlayerSignalEntity;
import net.droingo.aquietplace.signal.SignalType;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

public class PlayerSignalEntityRenderer extends EntityRenderer<PlayerSignalEntity> {
    private static final float SIGNAL_SIZE = 0.75f;
    private static final int FRAME_TICKS = 8;

    public PlayerSignalEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(
            PlayerSignalEntity entity,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light
    ) {
        matrices.push();

        matrices.multiply(this.dispatcher.getRotation());
        matrices.scale(SIGNAL_SIZE, SIGNAL_SIZE, SIGNAL_SIZE);

        Identifier texture = getAnimatedTexture(entity);
        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(texture));
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        float halfSize = 0.5f;

        vertices.vertex(matrix, -halfSize, -halfSize, 0.0f)
                .color(255, 255, 255, 255)
                .texture(0.0f, 1.0f)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(matrices.peek(), 0.0f, 1.0f, 0.0f);

        vertices.vertex(matrix, halfSize, -halfSize, 0.0f)
                .color(255, 255, 255, 255)
                .texture(1.0f, 1.0f)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(matrices.peek(), 0.0f, 1.0f, 0.0f);

        vertices.vertex(matrix, halfSize, halfSize, 0.0f)
                .color(255, 255, 255, 255)
                .texture(1.0f, 0.0f)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(matrices.peek(), 0.0f, 1.0f, 0.0f);

        vertices.vertex(matrix, -halfSize, halfSize, 0.0f)
                .color(255, 255, 255, 255)
                .texture(0.0f, 0.0f)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(matrices.peek(), 0.0f, 1.0f, 0.0f);

        matrices.pop();

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(PlayerSignalEntity entity) {
        return getAnimatedTexture(entity);
    }

    private Identifier getAnimatedTexture(PlayerSignalEntity entity) {
        SignalType signalType = entity.getSignalType();
        int frame = (entity.age / FRAME_TICKS) % 2;

        return Identifier.of(
                AQuietPlace.MOD_ID,
                "textures/gui/signal/signal_" + signalType.getId() + "_" + frame + ".png"
        );
    }
}