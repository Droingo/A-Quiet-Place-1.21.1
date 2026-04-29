package net.droingo.aquietplace.client.hud;

import net.droingo.aquietplace.config.QuietPlaceConfig;
import net.droingo.aquietplace.registry.ModBlocks;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class ClientItemTooltips {
    private ClientItemTooltips() {
    }

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipType, lines) -> {
            if (stack.isOf(ModBlocks.NOISEMAKER.asItem())) {
                addNoisemakerTooltip(lines);
                return;
            }

            if (stack.isOf(ModBlocks.NEWSPAPER_SOUNDPROOFING.asItem())) {
                addNewspaperTooltip(lines);
                return;
            }

            if (stack.isOf(ModBlocks.GLASS_BOTTLE_TRAP.asItem())) {
                addGlassBottleTrapTooltip(lines);
            }
        });
    }
    private static void addGlassBottleTrapTooltip(java.util.List<Text> lines) {
        if (!Screen.hasControlDown()) {
            lines.add(Text.translatable("tooltip.aquietplace.hold_ctrl")
                    .formatted(Formatting.DARK_GRAY));
            return;
        }

        QuietPlaceConfig.GlassBottleTrap config = QuietPlaceConfig.get().glassBottleTrap;

        lines.add(Text.translatable("tooltip.aquietplace.glass_bottle_trap.single_use")
                .formatted(Formatting.RED));

        lines.add(Text.translatable(
                        "tooltip.aquietplace.glass_bottle_trap.trigger_radius",
                        String.format("%.2f", config.triggerRadius)
                )
                .formatted(Formatting.GRAY));

        lines.add(Text.translatable(
                        "tooltip.aquietplace.glass_bottle_trap.noise_radius",
                        String.format("%.1f", config.noiseRadius)
                )
                .formatted(Formatting.GRAY));

        lines.add(Text.translatable("tooltip.aquietplace.glass_bottle_trap.attracts_death_angels")
                .formatted(Formatting.YELLOW));
    }

    private static void addNoisemakerTooltip(java.util.List<Text> lines) {
        if (!Screen.hasControlDown()) {
            lines.add(Text.translatable("tooltip.aquietplace.hold_ctrl")
                    .formatted(Formatting.DARK_GRAY));
            return;
        }

        lines.add(Text.translatable("tooltip.aquietplace.noisemaker.one_time_use")
                .formatted(Formatting.RED));

        lines.add(Text.translatable("tooltip.aquietplace.noisemaker.configure")
                .formatted(Formatting.GRAY));

        lines.add(Text.translatable("tooltip.aquietplace.noisemaker.redstone")
                .formatted(Formatting.GRAY));

        lines.add(Text.translatable("tooltip.aquietplace.noisemaker.use_cautiously")
                .formatted(Formatting.YELLOW));
    }

    private static void addNewspaperTooltip(java.util.List<Text> lines) {
        if (!Screen.hasControlDown()) {
            lines.add(Text.translatable("tooltip.aquietplace.hold_ctrl")
                    .formatted(Formatting.DARK_GRAY));
            return;
        }

        QuietPlaceConfig.Soundproofing config = QuietPlaceConfig.get().soundproofing;

        int maxReductionPercent = Math.round(config.maxRadiusReduction * 100.0f);

        lines.add(Text.translatable("tooltip.aquietplace.newspaper.suppresses_noise")
                .formatted(Formatting.GRAY));

        lines.add(Text.translatable(
                        "tooltip.aquietplace.newspaper.effective_range",
                        config.scanRadius
                )
                .formatted(Formatting.GRAY));

        lines.add(Text.translatable(
                        "tooltip.aquietplace.newspaper.max_reduction",
                        maxReductionPercent
                )
                .formatted(Formatting.GRAY));
    }
}