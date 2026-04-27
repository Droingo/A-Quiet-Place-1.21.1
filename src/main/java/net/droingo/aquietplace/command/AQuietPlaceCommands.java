package net.droingo.aquietplace.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
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
        ));
    }

    private static int emitDebugNoise(ServerCommandSource source, float radius) {
        Entity sourceEntity = source.getEntity();
        ServerPlayerEntity player = source.getPlayer();

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

        if (player != null) {
            player.sendMessage(Text.literal("Debug noise emitted with radius " + radius), false);
        }

        return 1;
    }
}