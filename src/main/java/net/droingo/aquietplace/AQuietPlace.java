package net.droingo.aquietplace;

import net.droingo.aquietplace.command.AQuietPlaceCommands;
import net.droingo.aquietplace.network.ModNetworking;
import net.droingo.aquietplace.noise.NoiseSystem;
import net.droingo.aquietplace.noise.PlayerNoiseHandler;
import net.droingo.aquietplace.registry.ModEntities;
import net.droingo.aquietplace.registry.ModItems;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.droingo.aquietplace.noise.InteractionNoiseHandler;
import net.droingo.aquietplace.noise.BlockNoiseHandler;
import net.droingo.aquietplace.noise.ActionNoiseHandler;
import net.droingo.aquietplace.config.QuietPlaceConfig;
import net.droingo.aquietplace.registry.ModBlocks;
import net.droingo.aquietplace.registry.ModBlockEntities;
import net.droingo.aquietplace.loot.ModLootTableModifiers;

public class AQuietPlace implements ModInitializer {
    public static final String MOD_ID = "aquietplace";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing A Quiet Place");

        QuietPlaceConfig.load();

        ModNetworking.registerPayloads();

        ModBlocks.register();
        ModBlockEntities.register();
        ModEntities.register();
        ModItems.register();
        ModLootTableModifiers.register();

        NoiseSystem.initialize();
        PlayerNoiseHandler.register();
        AQuietPlaceCommands.register();
        BlockNoiseHandler.register();
        InteractionNoiseHandler.register();
        ActionNoiseHandler.register();
    }
}