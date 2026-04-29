package net.droingo.aquietplace.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.droingo.aquietplace.config.QuietPlaceConfig;
import net.droingo.aquietplace.entity.deathangel.DeathAngelEntity;
import net.droingo.aquietplace.noise.NoiseEvent;
import net.droingo.aquietplace.noise.NoiseSystem;
import net.droingo.aquietplace.noise.NoiseType;
import net.droingo.aquietplace.registry.ModEntities;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class AQuietPlaceCommands {
    private AQuietPlaceCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                literal("aquietplace")
                        .requires(source -> source.hasPermissionLevel(2))

                        .then(literal("noise")
                                .then(argument("radius", FloatArgumentType.floatArg(0.0f, 256.0f))
                                        .executes(context -> emitDebugNoise(
                                                context.getSource(),
                                                FloatArgumentType.getFloat(context, "radius")
                                        ))
                                )
                        )

                        .then(literal("debug")
                                .then(literal("status")
                                        .executes(context -> sendDebugStatus(context.getSource()))
                                )

                                .then(literal("particles")
                                        .then(argument("enabled", BoolArgumentType.bool())
                                                .executes(context -> setDebugParticles(
                                                        context.getSource(),
                                                        BoolArgumentType.getBool(context, "enabled")
                                                ))
                                        )
                                )

                                .then(literal("logging")
                                        .then(argument("enabled", BoolArgumentType.bool())
                                                .executes(context -> setDebugLogging(
                                                        context.getSource(),
                                                        BoolArgumentType.getBool(context, "enabled")
                                                ))
                                        )
                                )
                        )
                        .then(literal("voice")
                                .then(literal("status")
                                        .executes(context -> sendVoiceStatus(context.getSource()))
                                )

                                .then(literal("enabled")
                                        .then(argument("enabled", BoolArgumentType.bool())
                                                .executes(context -> setVoiceNoiseEnabled(
                                                        context.getSource(),
                                                        BoolArgumentType.getBool(context, "enabled")
                                                ))
                                        )
                                )
                        )

                        .then(literal("config")
                                .then(literal("reload")
                                        .executes(context -> reloadConfig(context.getSource()))
                                )

                                .then(literal("save")
                                        .executes(context -> saveConfig(context.getSource()))
                                )

                                .then(literal("status")
                                        .executes(context -> sendConfigStatus(context.getSource()))
                                )

                                .then(literal("hunt_creative_players")
                                        .then(argument("enabled", BoolArgumentType.bool())
                                                .executes(context -> setHuntCreativePlayers(
                                                        context.getSource(),
                                                        BoolArgumentType.getBool(context, "enabled")
                                                ))
                                        )
                                )
                        )

                        .then(literal("admin")
                                .then(literal("spawn_death_angel")
                                        .executes(context -> spawnDeathAngels(context.getSource(), 1))
                                        .then(argument("count", IntegerArgumentType.integer(1, 50))
                                                .executes(context -> spawnDeathAngels(
                                                        context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "count")
                                                ))
                                        )
                                )

                                .then(literal("clear_death_angels")
                                        .executes(context -> clearDeathAngels(context.getSource()))
                                )

                                .then(literal("noise_at_block")
                                        .then(argument("pos", BlockPosArgumentType.blockPos())
                                                .then(argument("radius", FloatArgumentType.floatArg(0.0f, 256.0f))
                                                        .executes(context -> emitNoiseAtBlock(
                                                                context.getSource(),
                                                                BlockPosArgumentType.getBlockPos(context, "pos"),
                                                                FloatArgumentType.getFloat(context, "radius")
                                                        ))
                                                )
                                        )
                                )

                                .then(literal("force_hunt")
                                        .then(argument("player", EntityArgumentType.player())
                                                .executes(context -> forceHuntPlayer(
                                                        context.getSource(),
                                                        EntityArgumentType.getPlayer(context, "player")
                                                ))
                                        )
                                )

                                .then(literal("stop_hunts")
                                        .executes(context -> stopAllHunts(context.getSource()))
                                )
                        )
        ));
    }

    private static int emitDebugNoise(ServerCommandSource source, float radius) {
        Entity sourceEntity = source.getEntity();

        NoiseEvent noiseEvent = NoiseEvent.create(
                source.getWorld(),
                source.getPosition(),
                radius,
                radius,
                sourceEntity,
                NoiseType.COMMAND_DEBUG,
                true,
                true
        );

        NoiseSystem.emitNoise(noiseEvent);

        sendFeedback(source, "Debug noise emitted with radius " + radius);

        return 1;
    }
    private static int setVoiceNoiseEnabled(ServerCommandSource source, boolean enabled) {
        QuietPlaceConfig.get().voiceChatNoise.enabled = enabled;
        QuietPlaceConfig.save();

        sendFeedback(source, "Voice chat noise " + getEnabledText(enabled));
        sendFeedback(source, "Saved to config/aquietplace.json");

        return 1;
    }

    private static int sendVoiceStatus(ServerCommandSource source) {
        QuietPlaceConfig.VoiceChatNoise config = QuietPlaceConfig.get().voiceChatNoise;

        sendFeedback(source, "A Quiet Place Voice Chat Status:");
        sendFeedback(source, "Enabled: " + getEnabledText(config.enabled));
        sendFeedback(source, "Packet cooldown ticks: " + config.packetCooldownTicks);
        sendFeedback(source, "Minimum RMS: " + config.minimumRms);
        sendFeedback(source, "RMS strength multiplier: " + config.rmsStrengthMultiplier);
        sendFeedback(source, "Minimum strength: " + config.minimumStrength);
        sendFeedback(source, "Maximum strength: " + config.maximumStrength);
        sendFeedback(source, "Minimum radius: " + config.minimumRadius);
        sendFeedback(source, "Maximum radius: " + config.maximumRadius);
        sendFeedback(source, "Whisper radius multiplier: " + config.whisperRadiusMultiplier);
        sendFeedback(source, "Whisper strength multiplier: " + config.whisperStrengthMultiplier);

        return 1;
    }

    private static int emitNoiseAtBlock(ServerCommandSource source, BlockPos blockPos, float radius) {
        ServerWorld world = source.getWorld();
        Vec3d noisePosition = blockPos.toCenterPos();

        NoiseSystem.emitNoise(new NoiseEvent(
                world,
                noisePosition,
                radius,
                radius,
                null,
                NoiseType.COMMAND_DEBUG,
                true,
                true,
                world.getTime()
        ));

        sendFeedback(source, "Debug noise emitted at " + formatBlockPos(blockPos) + " with radius " + radius);

        return 1;
    }

    private static int spawnDeathAngels(ServerCommandSource source, int count) {
        ServerWorld world = source.getWorld();
        Vec3d position = source.getPosition();

        int spawned = 0;

        for (int index = 0; index < count; index++) {
            DeathAngelEntity deathAngel = ModEntities.DEATH_ANGEL.create(world);

            if (deathAngel == null) {
                continue;
            }

            double offsetX = (index % 5) - 2.0;
            double offsetZ = (index / 5) * 2.0;

            deathAngel.refreshPositionAndAngles(
                    position.x + offsetX,
                    position.y,
                    position.z + offsetZ,
                    source.getRotation().y,
                    0.0f
            );

            world.spawnEntity(deathAngel);
            spawned++;
        }

        sendFeedback(source, "Spawned " + spawned + " Death Angel(s)");

        return spawned;
    }

    private static int clearDeathAngels(ServerCommandSource source) {
        List<DeathAngelEntity> deathAngels = getDeathAngelsInWorld(source.getWorld());

        for (DeathAngelEntity deathAngel : deathAngels) {
            deathAngel.discard();
        }

        sendFeedback(source, "Cleared " + deathAngels.size() + " Death Angel(s)");

        return deathAngels.size();
    }

    private static int forceHuntPlayer(ServerCommandSource source, ServerPlayerEntity targetPlayer) {
        List<DeathAngelEntity> deathAngels = getDeathAngelsInWorld(source.getWorld());

        for (DeathAngelEntity deathAngel : deathAngels) {
            deathAngel.rememberNoisyTarget(targetPlayer.getUuid(), 20 * 10);
            deathAngel.suppressHearReaction(20 * 5);
        }

        sendFeedback(source, "Forced " + deathAngels.size() + " Death Angel(s) to hunt " + targetPlayer.getName().getString());

        return deathAngels.size();
    }

    private static int stopAllHunts(ServerCommandSource source) {
        List<DeathAngelEntity> deathAngels = getDeathAngelsInWorld(source.getWorld());

        for (DeathAngelEntity deathAngel : deathAngels) {
            deathAngel.clearHuntAndSearchMemory();
        }

        sendFeedback(source, "Stopped hunts/searches for " + deathAngels.size() + " Death Angel(s)");

        return deathAngels.size();
    }

    private static List<DeathAngelEntity> getDeathAngelsInWorld(ServerWorld world) {
        Box worldSizedBox = new Box(
                -30_000_000,
                -2048,
                -30_000_000,
                30_000_000,
                4096,
                30_000_000
        );

        return world.getEntitiesByClass(
                DeathAngelEntity.class,
                worldSizedBox,
                Entity::isAlive
        );
    }

    private static int setDebugParticles(ServerCommandSource source, boolean enabled) {
        NoiseSystem.setDebugParticlesEnabled(enabled);

        sendFeedback(source, "Noise debug particles " + getEnabledText(enabled));

        return 1;
    }

    private static int setDebugLogging(ServerCommandSource source, boolean enabled) {
        NoiseSystem.setDebugLoggingEnabled(enabled);

        sendFeedback(source, "Noise debug logging " + getEnabledText(enabled));

        return 1;
    }

    private static int setHuntCreativePlayers(ServerCommandSource source, boolean enabled) {
        QuietPlaceConfig.get().deathAngel.huntCreativePlayers = enabled;
        QuietPlaceConfig.save();

        sendFeedback(source, "Death Angels hunting Creative players " + getEnabledText(enabled));
        sendFeedback(source, "Saved to config/aquietplace.json");

        return 1;
    }

    private static int reloadConfig(ServerCommandSource source) {
        QuietPlaceConfig.load();

        NoiseSystem.setDebugParticlesEnabled(QuietPlaceConfig.get().debug.noiseParticlesEnabled);
        NoiseSystem.setDebugLoggingEnabled(QuietPlaceConfig.get().debug.noiseLoggingEnabled);

        sendFeedback(source, "Reloaded config/aquietplace.json");
        sendConfigStatus(source);

        return 1;
    }

    private static int saveConfig(ServerCommandSource source) {
        QuietPlaceConfig.save();

        sendFeedback(source, "Saved config/aquietplace.json");

        return 1;
    }

    private static int sendDebugStatus(ServerCommandSource source) {
        sendFeedback(source, "A Quiet Place Debug Status:");
        sendFeedback(source, "Noise particles: " + getEnabledText(NoiseSystem.isDebugParticlesEnabled()));
        sendFeedback(source, "Noise logging: " + getEnabledText(NoiseSystem.isDebugLoggingEnabled()));
        sendFeedback(source, "Hunt Creative players: " + getEnabledText(QuietPlaceConfig.get().deathAngel.huntCreativePlayers));

        return 1;
    }

    private static int sendConfigStatus(ServerCommandSource source) {
        QuietPlaceConfig config = QuietPlaceConfig.get();

        sendFeedback(source, "A Quiet Place Config Status:");
        sendFeedback(source, "Hunt Creative players: " + getEnabledText(config.deathAngel.huntCreativePlayers));
        sendFeedback(source, "Chase speed: " + config.deathAngel.chaseSpeed);
        sendFeedback(source, "Run attack speed: " + config.deathAngel.runAttackSpeed);
        sendFeedback(source, "Sprint radius: " + config.playerNoise.sprintRadius);
        sendFeedback(source, "Landing max radius: " + config.playerNoise.landingMaxRadius);
        sendFeedback(source, "Very loud radius threshold: " + config.deathAngel.veryLoudRadiusThreshold);
        sendFeedback(source, "Noise particles default: " + getEnabledText(config.debug.noiseParticlesEnabled));
        sendFeedback(source, "Noise logging default: " + getEnabledText(config.debug.noiseLoggingEnabled));

        return 1;
    }

    private static String formatBlockPos(BlockPos blockPos) {
        return blockPos.getX() + " " + blockPos.getY() + " " + blockPos.getZ();
    }

    private static String getEnabledText(boolean enabled) {
        return enabled ? "enabled" : "disabled";
    }

    private static void sendFeedback(ServerCommandSource source, String message) {
        Text text = Text.literal(message);

        if (source.getEntity() instanceof ServerPlayerEntity player) {
            player.sendMessage(text, false);
            return;
        }

        source.sendFeedback(() -> text, false);
    }
}