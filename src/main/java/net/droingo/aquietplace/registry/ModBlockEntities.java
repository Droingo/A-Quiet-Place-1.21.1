package net.droingo.aquietplace.registry;

import net.droingo.aquietplace.AQuietPlace;
import net.droingo.aquietplace.block.entity.NoisemakerBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModBlockEntities {
    public static final BlockEntityType<NoisemakerBlockEntity> NOISEMAKER = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(AQuietPlace.MOD_ID, "noisemaker"),
            FabricBlockEntityTypeBuilder.create(NoisemakerBlockEntity::new, ModBlocks.NOISEMAKER).build()
    );

    private ModBlockEntities() {
    }

    public static void register() {
        AQuietPlace.LOGGER.info("Registered block entities for {}", AQuietPlace.MOD_ID);
    }
}