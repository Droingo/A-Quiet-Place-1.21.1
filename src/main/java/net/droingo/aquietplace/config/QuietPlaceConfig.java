package net.droingo.aquietplace.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.droingo.aquietplace.AQuietPlace;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class QuietPlaceConfig {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("aquietplace.json");

    private static QuietPlaceConfig INSTANCE = new QuietPlaceConfig();

    public int configVersion = 1;

    public Debug debug = new Debug();
    public Hud hud = new Hud();
    public PlayerNoise playerNoise = new PlayerNoise();
    public DeathAngel deathAngel = new DeathAngel();

    public static QuietPlaceConfig get() {
        return INSTANCE;
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            INSTANCE = new QuietPlaceConfig();
            save();
            AQuietPlace.LOGGER.info("Created default A Quiet Place config at {}", CONFIG_PATH);
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            QuietPlaceConfig loadedConfig = GSON.fromJson(reader, QuietPlaceConfig.class);

            if (loadedConfig == null) {
                AQuietPlace.LOGGER.warn("A Quiet Place config was empty. Using defaults.");
                INSTANCE = new QuietPlaceConfig();
                save();
                return;
            }

            INSTANCE = loadedConfig;
            INSTANCE.fillMissingSections();

            AQuietPlace.LOGGER.info("Loaded A Quiet Place config from {}", CONFIG_PATH);
        } catch (Exception exception) {
            AQuietPlace.LOGGER.error("Failed to load A Quiet Place config. Using defaults.", exception);
            INSTANCE = new QuietPlaceConfig();
            save();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (IOException exception) {
            AQuietPlace.LOGGER.error("Failed to save A Quiet Place config.", exception);
        }
    }

    private void fillMissingSections() {
        if (this.debug == null) {
            this.debug = new Debug();
        }

        if (this.hud == null) {
            this.hud = new Hud();
        }

        if (this.playerNoise == null) {
            this.playerNoise = new PlayerNoise();
        }

        if (this.deathAngel == null) {
            this.deathAngel = new DeathAngel();
        }
    }

    public static class Debug {
        public boolean noiseParticlesEnabled = true;
        public boolean noiseLoggingEnabled = true;
        public boolean deathAngelAmbientParticlesEnabled = true;
    }

    public static class Hud {
        public int packetIntervalTicks = 2;
        public int noiseHoldTicks = 12;
        public float noiseDecayPerTick = 0.025f;
    }

    public static class PlayerNoise {
        public double minHorizontalDistancePerTick = 0.015;

        public int walkIntervalTicks = 14;
        public float walkStrength = 0.45f;
        public float walkRadius = 5.0f;
        public float walkHudLevel = 0.45f;

        public int sprintIntervalTicks = 8;
        public float sprintStrength = 1.0f;
        public float sprintRadius = 18.0f;
        public float sprintHudLevel = 1.0f;

        public int sneakIntervalTicks = 24;
        public float sneakStrength = 0.25f;
        public float sneakRadius = 1.5f;
        public float sneakHudLevel = 0.15f;

        public float jumpStrength = 0.65f;
        public float jumpRadius = 8.0f;
        public float jumpHudLevel = 0.65f;

        public int minimumLandingAirTicks = 6;
        public float landingBaseRadius = 4.0f;
        public float landingRadiusPerAirTick = 0.75f;
        public float landingMaxRadius = 30.0f;
        public float landingBaseStrength = 0.35f;
        public float landingStrengthPerAirTick = 0.035f;
        public float landingMaxStrength = 1.0f;
    }

    public static class DeathAngel {
        public boolean huntCreativePlayers = true;

        public double maxHealth = 80.0;
        public double baseMovementSpeed = 0.30;
        public double attackDamage = 12.0;
        public double followRange = 48.0;
        public double knockbackResistance = 0.8;

        public double wanderSpeed = 0.85;
        public double investigateSpeed = 1.15;
        public double searchSpeed = 1.25;
        public double chaseSpeed = 1.95;
        public double runAttackSpeed = 1.75;

        public int attackWindupTicks = 7;
        public int runAttackAnimationTicks = 18;
        public int attackCooldownTicks = 18;
        public int postAttackMemoryTicks = 100;
        public int postAttackSuppressHearTicks = 100;
    }
}