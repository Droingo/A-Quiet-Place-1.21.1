package net.droingo.aquietplace.registry;

import net.droingo.aquietplace.AQuietPlace;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModItems {
    public static final Item DEATH_ANGEL_SPAWN_EGG = Registry.register(
            Registries.ITEM,
            Identifier.of(AQuietPlace.MOD_ID, "death_angel_spawn_egg"),
            new SpawnEggItem(
                    ModEntities.DEATH_ANGEL,
                    0x1B1B1B,
                    0x8B0000,
                    new Item.Settings()
            )
    );

    public static final Item SCRAP_ELECTRONICS = registerItem(
            "scrap_electronics",
            new Item(new Item.Settings())
    );

    private static Item registerItem(String name, Item item) {
        return Registry.register(
                Registries.ITEM,
                Identifier.of(AQuietPlace.MOD_ID, name),
                item
        );
    }

    private ModItems() {
    }

    public static void register() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(entries -> entries.add(DEATH_ANGEL_SPAWN_EGG));

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries ->
                entries.add(SCRAP_ELECTRONICS)
        );

        AQuietPlace.LOGGER.info("Registered items for {}", AQuietPlace.MOD_ID);
    }
}