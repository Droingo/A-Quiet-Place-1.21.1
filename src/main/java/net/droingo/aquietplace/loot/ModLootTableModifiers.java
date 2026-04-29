package net.droingo.aquietplace.loot;

import net.droingo.aquietplace.AQuietPlace;
import net.droingo.aquietplace.registry.ModItems;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.Set;

public final class ModLootTableModifiers {
    private static final Set<RegistryKey<LootTable>> SCRAP_ELECTRONICS_LOOT_TABLES = Set.of(
            vanillaLootTable("chests/simple_dungeon"),
            vanillaLootTable("chests/abandoned_mineshaft"),
            vanillaLootTable("chests/stronghold_corridor"),
            vanillaLootTable("chests/village/village_toolsmith"),
            vanillaLootTable("chests/village/village_weaponsmith"),
            vanillaLootTable("chests/ruined_portal"),
            vanillaLootTable("chests/ancient_city")
    );

    private ModLootTableModifiers() {
    }

    public static void register() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (!source.isBuiltin()) {
                return;
            }

            if (!SCRAP_ELECTRONICS_LOOT_TABLES.contains(key)) {
                return;
            }

            LootPool.Builder pool = LootPool.builder()
                    .rolls(UniformLootNumberProvider.create(1.0f, 3.0f))
                    .conditionally(RandomChanceLootCondition.builder(0.35f))
                    .with(ItemEntry.builder(ModItems.SCRAP_ELECTRONICS));

            tableBuilder.pool(pool);
        });

        AQuietPlace.LOGGER.info("Registered loot table modifiers for {}", AQuietPlace.MOD_ID);
    }

    private static RegistryKey<LootTable> vanillaLootTable(String path) {
        return RegistryKey.of(
                RegistryKeys.LOOT_TABLE,
                Identifier.ofVanilla(path)
        );
    }
}