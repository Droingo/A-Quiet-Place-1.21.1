package net.droingo.aquietplace.client.signal;

import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ClientSignalColorSettings {
    private static final int DEFAULT_SKIN_COLOR = 0xF1C27D;

    private static int selectedColorRgb = DEFAULT_SKIN_COLOR;
    private static boolean loaded = false;

    private ClientSignalColorSettings() {
    }

    public static void load() {
        if (loaded) {
            return;
        }

        loaded = true;

        Path configPath = getConfigPath();

        if (!Files.exists(configPath)) {
            save();
            return;
        }

        Properties properties = new Properties();

        try (InputStream inputStream = Files.newInputStream(configPath)) {
            properties.load(inputStream);

            String rawColor = properties.getProperty("signalSkinColor", Integer.toHexString(DEFAULT_SKIN_COLOR));

            selectedColorRgb = parseRgb(rawColor, DEFAULT_SKIN_COLOR);
        } catch (IOException exception) {
            selectedColorRgb = DEFAULT_SKIN_COLOR;
        }
    }

    public static int getSelectedColorRgb() {
        load();
        return selectedColorRgb;
    }

    public static void setSelectedColorRgb(int colorRgb) {
        selectedColorRgb = colorRgb & 0xFFFFFF;
        save();
    }

    private static void save() {
        Path configPath = getConfigPath();

        try {
            Files.createDirectories(configPath.getParent());

            Properties properties = new Properties();
            properties.setProperty("signalSkinColor", String.format("%06X", selectedColorRgb));

            try (OutputStream outputStream = Files.newOutputStream(configPath)) {
                properties.store(outputStream, "A Quiet Place signal settings");
            }
        } catch (IOException ignored) {
            // Non-critical. If saving fails, the selected color still works for this session.
        }
    }

    private static Path getConfigPath() {
        return MinecraftClient.getInstance()
                .runDirectory
                .toPath()
                .resolve("config")
                .resolve("aquietplace_signal_color.properties");
    }

    private static int parseRgb(String rawColor, int fallback) {
        if (rawColor == null) {
            return fallback;
        }

        String cleaned = rawColor.trim();

        if (cleaned.startsWith("#")) {
            cleaned = cleaned.substring(1);
        }

        try {
            return Integer.parseInt(cleaned, 16) & 0xFFFFFF;
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}