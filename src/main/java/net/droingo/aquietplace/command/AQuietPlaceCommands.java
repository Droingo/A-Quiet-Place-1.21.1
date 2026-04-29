package net.droingo.aquietplace.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.droingo.aquietplace.config.QuietPlaceConfig;
import net.droingo.aquietplace.noise.NoiseEvent;
import net.droingo.aquietplace.noise.NoiseSystem;
import net.droingo.aquietplace.noise.NoiseType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

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

                        .then(literal("config")
                                .then(literal("hunt_creative_players")
                                        .then(argument("enabled", BoolArgumentType.bool())
                                                .executes(context -> setHuntCreativePlayers(
                                                        context.getSource(),
                                                        BoolArgumentType.getBool(context, "enabled")
                                                ))
                                        )
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

    private static int sendDebugStatus(ServerCommandSource source) {
        sendFeedback(source, "A Quiet Place Debug Status:");
        sendFeedback(source, "Noise particles: " + getEnabledText(NoiseSystem.isDebugParticlesEnabled()));
        sendFeedback(source, "Noise logging: " + getEnabledText(NoiseSystem.isDebugLoggingEnabled()));
        sendFeedback(source, "Hunt Creative players: " + getEnabledText(QuietPlaceConfig.get().deathAngel.huntCreativePlayers));

        return 1;
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