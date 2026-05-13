package net.droingo.aquietplace.item;

import net.droingo.aquietplace.config.QuietPlaceConfig;
import net.droingo.aquietplace.entity.deathangel.DeathAngelEntity;
import net.droingo.aquietplace.noise.NoiseEvent;
import net.droingo.aquietplace.noise.NoiseSystem;
import net.droingo.aquietplace.noise.NoiseType;
import net.droingo.aquietplace.registry.ModItems;
import net.droingo.aquietplace.registry.ModSounds;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.droingo.aquietplace.AQuietPlace;
import net.minecraft.network.packet.s2c.play.StopSoundS2CPacket;
import net.minecraft.util.Identifier;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public final class FeedbackEmitterPulseManager {
    private static final int TOTAL_DURATION_TICKS = 220;
    private static final int[] PULSE_TICKS = {0, 73, 146, 219};

    private static final List<ActivePulse> ACTIVE_PULSES = new ArrayList<>();
    private static boolean registered = false;

    private FeedbackEmitterPulseManager() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        registered = true;
        ServerTickEvents.END_SERVER_TICK.register(FeedbackEmitterPulseManager::tick);
    }

    public static void start(
            ServerWorld world,
            PlayerEntity user,
            Hand hand,
            QuietPlaceConfig.FeedbackEmitter config
    ) {
        ActivePulse pulse = new ActivePulse(
                world,
                user.getUuid(),
                hand,
                user.getPos(),
                user.getRotationVec(1.0F).normalize(),
                config.stunRadius,
                config.stunDurationTicks,
                config.noiseStrength,
                config.noiseRadius,
                (float) config.soundVolume,
                (float) config.soundPitch,
                config.particleCount
        );

        ACTIVE_PULSES.add(pulse);

        // Fire the first pulse immediately so the item always feels responsive,
        // even before the next server tick.
        pulse.tryFireDuePulses();
    }

    private static void tick(MinecraftServer server) {
        Iterator<ActivePulse> iterator = ACTIVE_PULSES.iterator();

        while (iterator.hasNext()) {
            ActivePulse pulse = iterator.next();

            if (pulse.world.getServer() != server) {
                iterator.remove();
                continue;
            }

            pulse.tick();

            if (pulse.isFinished()) {
                iterator.remove();
            }
        }
    }

    private static final class ActivePulse {
        private final ServerWorld world;
        private final UUID userUuid;
        private final Hand hand;
        private final Vec3d origin;
        private final Vec3d direction;



        private final double stunRadius;
        private final int stunDurationTicks;
        private final double noiseStrength;
        private final double noiseRadius;
        private final float soundVolume;
        private final float soundPitch;
        private final int particleCount;

        private int ageTicks = 0;
        private int nextPulseIndex = 0;
        private boolean breakSoundPlayed = false;
        private boolean itemBroken = false;

        private final Set<UUID> startedDeathAngelReactions = new HashSet<>();

        private ActivePulse(
                ServerWorld world,
                UUID userUuid,
                Hand hand,
                Vec3d origin,
                Vec3d direction,
                double stunRadius,
                int stunDurationTicks,
                double noiseStrength,
                double noiseRadius,
                float soundVolume,
                float soundPitch,
                int particleCount
        ) {
            this.world = world;
            this.userUuid = userUuid;
            this.hand = hand;
            this.origin = origin;
            this.direction = direction.lengthSquared() < 0.001 ? new Vec3d(0.0, 0.0, 1.0) : direction;
            this.stunRadius = stunRadius;
            this.stunDurationTicks = stunDurationTicks;
            this.noiseStrength = noiseStrength;
            this.noiseRadius = noiseRadius;
            this.soundVolume = soundVolume;
            this.soundPitch = soundPitch;
            this.particleCount = particleCount;
        }

        private void stopHighPitchSoundForNearbyPlayers(Vec3d pos) {
            Identifier soundId = Identifier.of(AQuietPlace.MOD_ID, "item.high_pitch_stun");
            StopSoundS2CPacket packet = new StopSoundS2CPacket(soundId, SoundCategory.PLAYERS);

            for (ServerPlayerEntity player : world.getPlayers()) {
                if (player.squaredDistanceTo(pos) <= 96.0 * 96.0) {
                    player.networkHandler.sendPacket(packet);
                }
            }
        }

        private void tick() {
            tryFireDuePulses();

            if (!breakSoundPlayed && ageTicks >= TOTAL_DURATION_TICKS) {
                playBreakEffectsAndConsumeItem();
                breakSoundPlayed = true;
                itemBroken = true;
            }

            ageTicks++;
        }

        private void tryFireDuePulses() {
            while (nextPulseIndex < PULSE_TICKS.length && ageTicks >= PULSE_TICKS[nextPulseIndex]) {
                firePulse(nextPulseIndex);
                nextPulseIndex++;
            }
        }

        private boolean isFinished() {
            return itemBroken && ageTicks > TOTAL_DURATION_TICKS + 10;
        }

        private void firePulse(int pulseIndex) {
            ServerPlayerEntity user = getUser();

            Vec3d currentOrigin = user != null ? user.getPos() : origin;
            Vec3d eyePos = user != null ? user.getEyePos() : currentOrigin.add(0.0, 1.5, 0.0);
            Vec3d currentDirection = user != null ? user.getRotationVec(1.0F).normalize() : direction;

            if (pulseIndex == 0) {
                world.playSound(
                        null,
                        currentOrigin.x,
                        currentOrigin.y,
                        currentOrigin.z,
                        ModSounds.HIGH_PITCH_STUN,
                        SoundCategory.PLAYERS,
                        soundVolume,
                        soundPitch
                );
            }

            emitFeedbackNoise(currentOrigin);
            spawnEmitterSparks(eyePos);
            spawnDirectionalSonicBurst(eyePos, currentDirection, pulseIndex);

            int affectedCount = affectNearbyDeathAngels(currentOrigin);

            if (user != null) {
                user.sendMessage(
                        Text.literal("Feedback pulse " + (pulseIndex + 1) + "/4 — Death Angels affected: " + affectedCount),
                        true
                );
            }
        }

        private int affectNearbyDeathAngels(Vec3d currentOrigin) {
            Box searchBox = new Box(
                    currentOrigin.x - stunRadius,
                    currentOrigin.y - stunRadius,
                    currentOrigin.z - stunRadius,
                    currentOrigin.x + stunRadius,
                    currentOrigin.y + stunRadius,
                    currentOrigin.z + stunRadius
            );

            List<DeathAngelEntity> deathAngels = world.getEntitiesByClass(
                    DeathAngelEntity.class,
                    searchBox,
                    deathAngel -> deathAngel.isAlive()
                            && deathAngel.squaredDistanceTo(currentOrigin) <= stunRadius * stunRadius
            );

            for (DeathAngelEntity deathAngel : deathAngels) {
                UUID deathAngelUuid = deathAngel.getUuid();

                if (!startedDeathAngelReactions.contains(deathAngelUuid)) {
                    deathAngel.stunFromHighPitch(stunDurationTicks, currentOrigin);
                    startedDeathAngelReactions.add(deathAngelUuid);
                }

                playDeathAngelStunSound(deathAngel);
                spawnDeathAngelSonicPulse(deathAngel);
            }

            return deathAngels.size();
        }

        private void emitFeedbackNoise(Vec3d currentOrigin) {
            NoiseSystem.emitNoise(new NoiseEvent(
                    world,
                    currentOrigin,
                    (float) noiseStrength,
                    (float) noiseRadius,
                    userUuid,
                    NoiseType.COMMAND_DEBUG,
                    true,
                    true,
                    world.getTime()
            ));
        }

        private void playDeathAngelStunSound(DeathAngelEntity deathAngel) {
            world.playSound(
                    null,
                    deathAngel.getX(),
                    deathAngel.getY(),
                    deathAngel.getZ(),
                    ModSounds.DEATH_ANGEL_HURT,
                    SoundCategory.HOSTILE,
                    Math.max(1.0F, soundVolume),
                    0.65F
            );
        }

        private void spawnEmitterSparks(Vec3d eyePos) {
            world.spawnParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    eyePos.x,
                    eyePos.y - 0.25,
                    eyePos.z,
                    Math.max(8, particleCount / 2),
                    0.25,
                    0.18,
                    0.25,
                    0.08
            );

            world.spawnParticles(
                    ParticleTypes.CRIT,
                    eyePos.x,
                    eyePos.y - 0.25,
                    eyePos.z,
                    Math.max(6, particleCount / 3),
                    0.2,
                    0.12,
                    0.2,
                    0.05
            );
        }

        private void spawnDirectionalSonicBurst(Vec3d eyePos, Vec3d currentDirection, int pulseIndex) {
            int rings = 8;
            double spacing = 1.15 + pulseIndex * 0.2;

            for (int ring = 1; ring <= rings; ring++) {
                Vec3d point = eyePos.add(currentDirection.multiply(ring * spacing));

                world.spawnParticles(
                        ParticleTypes.SONIC_BOOM,
                        point.x,
                        point.y,
                        point.z,
                        1,
                        0.03,
                        0.03,
                        0.03,
                        0.0
                );

                world.spawnParticles(
                        ParticleTypes.END_ROD,
                        point.x,
                        point.y,
                        point.z,
                        5,
                        0.25,
                        0.25,
                        0.25,
                        0.015
                );
            }
        }

        private void spawnDeathAngelSonicPulse(DeathAngelEntity deathAngel) {
            Vec3d pos = deathAngel.getPos().add(0.0, deathAngel.getHeight() * 0.65, 0.0);

            world.spawnParticles(
                    ParticleTypes.SONIC_BOOM,
                    pos.x,
                    pos.y,
                    pos.z,
                    2,
                    0.45,
                    0.45,
                    0.45,
                    0.0
            );

            world.spawnParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    pos.x,
                    pos.y,
                    pos.z,
                    18,
                    0.65,
                    0.65,
                    0.65,
                    0.08
            );
        }

        private void playBreakEffectsAndConsumeItem() {
            ServerPlayerEntity user = getUser();
            Vec3d pos = user != null ? user.getPos() : origin;
            stopHighPitchSoundForNearbyPlayers(pos);

            world.playSound(
                    null,
                    pos.x,
                    pos.y,
                    pos.z,
                    SoundEvents.ENTITY_ITEM_BREAK,
                    SoundCategory.PLAYERS,
                    1.0F,
                    0.8F
            );

            world.spawnParticles(
                    ParticleTypes.SMOKE,
                    pos.x,
                    pos.y + 1.0,
                    pos.z,
                    12,
                    0.25,
                    0.25,
                    0.25,
                    0.03
            );

            world.spawnParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    pos.x,
                    pos.y + 1.0,
                    pos.z,
                    16,
                    0.25,
                    0.25,
                    0.25,
                    0.12
            );

            if (user != null && !user.getAbilities().creativeMode) {
                consumeFeedbackEmitter(user);
            }

            if (user != null) {
                user.sendMessage(Text.literal("The Feedback Emitter burns out."), true);
            }
        }

        private void consumeFeedbackEmitter(ServerPlayerEntity user) {
            ItemStack handStack = user.getStackInHand(hand);

            if (handStack.isOf(ModItems.FEEDBACK_EMITTER)) {
                handStack.decrement(1);
                return;
            }

            for (int slot = 0; slot < user.getInventory().size(); slot++) {
                ItemStack stack = user.getInventory().getStack(slot);

                if (stack.isOf(ModItems.FEEDBACK_EMITTER)) {
                    stack.decrement(1);
                    return;
                }
            }
        }

        private ServerPlayerEntity getUser() {
            return world.getServer().getPlayerManager().getPlayer(userUuid);
        }
    }
}