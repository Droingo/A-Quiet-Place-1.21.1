package net.droingo.aquietplace.registry;

import net.droingo.aquietplace.AQuietPlace;
import net.droingo.aquietplace.block.NewspaperSoundproofingBlock;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.droingo.aquietplace.item.NoisemakerBlockItem;
import net.droingo.aquietplace.block.NoisemakerBlock;
import net.droingo.aquietplace.block.GlassBottleTrapBlock;

public final class ModBlocks {
    public static final Block NEWSPAPER_SOUNDPROOFING = registerBlockWithItem(
            "newspaper_soundproofing",
            new NewspaperSoundproofingBlock(AbstractBlock.Settings.copy(Blocks.WHITE_WOOL)
                    .sounds(BlockSoundGroup.WOOL)
                    .nonOpaque()
                    .noCollision()
                    .strength(0.2f)
            )
    );
    public static final Block GLASS_BOTTLE_TRAP = registerBlockWithItem(
            "glass_bottle_trap",
            new GlassBottleTrapBlock(AbstractBlock.Settings.copy(Blocks.GLASS)
                    .nonOpaque()
                    .strength(0.3f)
            )
    );
    public static final Block NOISEMAKER = registerBlockWithItem(
            "noisemaker",
            new NoisemakerBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)
                    .nonOpaque()
                    .strength(1.5f)
            )
    );

    private ModBlocks() {
    }

    public static void register() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {
            entries.add(NEWSPAPER_SOUNDPROOFING);
            entries.add(NOISEMAKER);
            entries.add(GLASS_BOTTLE_TRAP);
        });

        AQuietPlace.LOGGER.info("Registered blocks for {}", AQuietPlace.MOD_ID);
    }

    private static Block registerBlockWithItem(String name, Block block) {
        Registry.register(
                Registries.BLOCK,
                Identifier.of(AQuietPlace.MOD_ID, name),
                block
        );

        Item blockItem = name.equals("noisemaker")
                ? new NoisemakerBlockItem(block, new Item.Settings())
                : new BlockItem(block, new Item.Settings());

        Registry.register(
                Registries.ITEM,
                Identifier.of(AQuietPlace.MOD_ID, name),
                blockItem
        );

        return block;
    }
}