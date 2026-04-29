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
    public InteractionNoise interactionNoise = new InteractionNoise();
    public BlockNoise blockNoise = new BlockNoise();
    public ActionNoise actionNoise = new ActionNoise();
    public DeathAngel deathAngel = new DeathAngel();
    public VoiceChatNoise voiceChatNoise = new VoiceChatNoise();
    public Soundproofing soundproofing = new Soundproofing();

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
            save();

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
        if (this.voiceChatNoise == null) {
            this.voiceChatNoise = new VoiceChatNoise();
        }

        if (this.hud == null) {
            this.hud = new Hud();
        }

        if (this.playerNoise == null) {
            this.playerNoise = new PlayerNoise();
        }

        if (this.interactionNoise == null) {
            this.interactionNoise = new InteractionNoise();
        }

        if (this.blockNoise == null) {
            this.blockNoise = new BlockNoise();
        }

        if (this.actionNoise == null) {
            this.actionNoise = new ActionNoise();
        }

        if (this.deathAngel == null) {
            this.deathAngel = new DeathAngel();
        }
        if (this.soundproofing == null) {
            this.soundproofing = new Soundproofing();
        }
    }

    public static class Debug {
        public boolean noiseParticlesEnabled = true;
        public boolean noiseLoggingEnabled = true;
        public boolean deathAngelAmbientParticlesEnabled = true;
    }

    public static class VoiceChatNoise {
        public boolean enabled = true;

        /*
         * How often voice packets can create gameplay noise per player.
         * 5 ticks = up to 4 voice noise events per second per speaking player.
         */
        public int packetCooldownTicks = 5;

        /*
         * Ignore extremely quiet decoded audio.
         * Raise this if breathing/background noise triggers too much.
         */
        public float minimumRms = 0.015f;

        /*
         * Converts decoded microphone RMS into gameplay strength.
         * Higher = voice becomes dangerous faster.
         */
        public float rmsStrengthMultiplier = 8.0f;

        public float minimumStrength = 0.20f;
        public float maximumStrength = 1.0f;

        public float minimumRadius = 3.0f;
        public float maximumRadius = 28.0f;

        /*
         * Whispering through Simple Voice Chat is quieter.
         */
        public float whisperRadiusMultiplier = 0.45f;
        public float whisperStrengthMultiplier = 0.60f;

        /*
         * HUD level multiplier. Usually same as strength.
         */
        public float hudMultiplier = 1.0f;
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
    public static class Soundproofing {
        public boolean enabled = true;

        /*
         * How far around a noise source to scan for newspaper panels.
         * 3 means a 7x7x7 cube, but the scan exits early once max reduction is reached.
         */
        public int scanRadius = 3;

        /*
         * Radius reduction controls how far Death Angels can hear the sound.
         */
        public float radiusReductionPerPanel = 0.08f;
        public float maxRadiusReduction = 0.75f;
        public float minimumRadiusMultiplier = 0.25f;

        /*
         * Strength reduction affects HUD level/priority-style logic if strength is used.
         */
        public float strengthReductionPerPanel = 0.04f;
        public float maxStrengthReduction = 0.50f;
        public float minimumStrengthMultiplier = 0.50f;

        /*
         * Safety floors so soundproofing never turns every sound into true zero.
         */
        public float minimumFinalRadius = 0.75f;
        public float minimumFinalStrength = 0.05f;
    }

    public static class InteractionNoise {
        public float quietRadiusMultiplier = 0.20f;
        public float quietStrengthMultiplier = 0.35f;

        public float doorStrength = 0.80f;
        public float doorRadius = 14.0f;
        public float doorHudLevel = 0.70f;

        public float trapdoorStrength = 0.65f;
        public float trapdoorRadius = 11.0f;
        public float trapdoorHudLevel = 0.60f;

        public float fenceGateStrength = 0.65f;
        public float fenceGateRadius = 11.0f;
        public float fenceGateHudLevel = 0.60f;

        public float containerStrength = 0.55f;
        public float containerRadius = 9.0f;
        public float containerHudLevel = 0.55f;

        public float buttonStrength = 0.45f;
        public float buttonRadius = 7.0f;
        public float buttonHudLevel = 0.45f;

        public float leverStrength = 0.55f;
        public float leverRadius = 9.0f;
        public float leverHudLevel = 0.55f;
    }

    public static class BlockNoise {
        public float quietPlaceRadiusMultiplier = 0.35f;
        public float quietPlaceStrengthMultiplier = 0.50f;

        public float placeStrength = 0.45f;
        public float placeRadius = 7.0f;
        public float placeHudLevel = 0.45f;

        public float softBreakStrength = 0.60f;
        public float softBreakRadius = 12.0f;

        public float mediumBreakStrength = 0.75f;
        public float mediumBreakRadius = 16.0f;

        public float hardBreakStrength = 0.95f;
        public float hardBreakRadius = 22.0f;

        public float mediumHardnessThreshold = 2.0f;
        public float hardHardnessThreshold = 5.0f;
    }

    public static class ActionNoise {
        public int eatingNoiseIntervalTicks = 12;
        public int dropScanIntervalTicks = 5;
        public double droppedItemScanRadius = 24.0;

        public float attackStrength = 0.85f;
        public float attackRadius = 14.0f;

        public float damageBaseStrength = 0.55f;
        public float damageStrengthPerHealthLost = 0.08f;
        public float damageMaxStrength = 1.0f;

        public float damageBaseRadius = 9.0f;
        public float damageRadiusPerHealthLost = 2.0f;
        public float damageMaxRadius = 24.0f;

        public float eatStrength = 0.35f;
        public float eatRadius = 5.0f;

        public float dropItemPlayerStrength = 0.40f;
        public float dropItemPlayerRadius = 6.0f;

        public float dropItemUnknownStrength = 0.30f;
        public float dropItemUnknownRadius = 4.0f;
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

        public float huntStrengthThreshold = 0.8f;
        public float huntRadiusThreshold = 12.0f;

        public float dangerousStrengthThreshold = 0.95f;
        public float dangerousRadiusThreshold = 18.0f;

        public float veryLoudStrengthThreshold = 1.0f;
        public float veryLoudRadiusThreshold = 22.0f;

        public int attackWindupTicks = 7;
        public int runAttackAnimationTicks = 18;
        public int attackCooldownTicks = 18;
        public int postAttackMemoryTicks = 100;
        public int postAttackSuppressHearTicks = 100;
    }
}