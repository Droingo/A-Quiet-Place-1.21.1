package net.droingo.aquietplace;

import net.droingo.aquietplace.command.AQuietPlaceCommands;
import net.droingo.aquietplace.noise.NoiseSystem;
import net.droingo.aquietplace.registry.ModEntities;
import net.droingo.aquietplace.registry.ModItems;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AQuietPlace implements ModInitializer {
    public static final String MOD_ID = "aquietplace";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing A Quiet Place");

        ModEntities.register();
        ModItems.register();

        NoiseSystem.initialize();
        AQuietPlaceCommands.register();
    }
}