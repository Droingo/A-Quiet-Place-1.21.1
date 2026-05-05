package net.droingo.aquietplace.item;

import net.droingo.aquietplace.config.QuietPlaceConfig;
import net.droingo.aquietplace.entity.deathangel.DeathAngelEntity;
import net.droingo.aquietplace.noise.NoiseEvent;
import net.droingo.aquietplace.noise.NoiseSystem;
import net.droingo.aquietplace.noise.NoiseType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public class FeedbackEmitterItem extends Item {
    public FeedbackEmitterItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient()) {
            return TypedActionResult.success(stack);
        }

        if (!(world instanceof ServerWorld serverWorld)) {
            return TypedActionResult.pass(stack);
        }

        QuietPlaceConfig.FeedbackEmitter config = QuietPlaceConfig.get().feedbackEmitter;

        if (!config.enabled) {
            user.sendMessage(Text.literal("The Feedback Emitter is disabled."), true);
            return TypedActionResult.fail(stack);
        }

        int stunnedCount = stunNearbyDeathAngels(serverWorld, user, config);

        emitFeedbackNoise(serverWorld, user, config);
        playFeedbackEffects(serverWorld, user, config);

        if (!user.getAbilities().creativeMode) {
            stack.decrement(1);
        }

        user.getItemCooldownManager().set(this, 20);

        user.sendMessage(
                Text.literal("Feedback burst fired. Death Angels stunned: " + stunnedCount),
                true
        );

        return TypedActionResult.success(stack);
    }

    private int stunNearbyDeathAngels(
            ServerWorld world,
            PlayerEntity user,
            QuietPlaceConfig.FeedbackEmitter config
    ) {
        double radius = config.stunRadius;
        Box searchBox = user.getBoundingBox().expand(radius);

        List<DeathAngelEntity> deathAngels = world.getEntitiesByClass(
                DeathAngelEntity.class,
                searchBox,
                deathAngel -> deathAngel.isAlive()
                        && deathAngel.squaredDistanceTo(user) <= radius * radius
        );

        for (DeathAngelEntity deathAngel : deathAngels) {
            deathAngel.stunFromHighPitch(config.stunDurationTicks, user.getPos());
        }

        return deathAngels.size();
    }

    private void emitFeedbackNoise(
            ServerWorld world,
            PlayerEntity user,
            QuietPlaceConfig.FeedbackEmitter config
    ) {
        NoiseSystem.emitNoise(new NoiseEvent(
                world,
                user.getPos(),
                config.noiseStrength,
                config.noiseRadius,
                user.getUuid(),
                NoiseType.COMMAND_DEBUG,
                true,
                true,
                world.getTime()
        ));
    }

    private void playFeedbackEffects(
            ServerWorld world,
            PlayerEntity user,
            QuietPlaceConfig.FeedbackEmitter config
    ) {
        Vec3d pos = user.getPos().add(0.0, 1.0, 0.0);

        world.playSound(
                null,
                user.getBlockPos(),
                SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                SoundCategory.PLAYERS,
                config.soundVolume,
                config.soundPitch
        );

        world.spawnParticles(
                ParticleTypes.SONIC_BOOM,
                pos.x,
                pos.y,
                pos.z,
                config.particleCount,
                0.35,
                0.35,
                0.35,
                0.0
        );
    }
}