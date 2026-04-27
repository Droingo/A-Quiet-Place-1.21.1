package net.droingo.aquietplace.registry;

import net.droingo.aquietplace.AQuietPlace;
import net.droingo.aquietplace.entity.deathangel.DeathAngelEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModEntities {
    public static final EntityType<DeathAngelEntity> DEATH_ANGEL = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(AQuietPlace.MOD_ID, "death_angel"),
            EntityType.Builder.create(DeathAngelEntity::new, SpawnGroup.MONSTER)
                    .dimensions(1.4f, 2.7f)
                    .eyeHeight(2.3f)
                    .maxTrackingRange(10)
                    .trackingTickInterval(3)
                    .build("death_angel")
    );

    private ModEntities() {
    }

    public static void register() {
        FabricDefaultAttributeRegistry.register(DEATH_ANGEL, DeathAngelEntity.createDeathAngelAttributes());

        AQuietPlace.LOGGER.info("Registered entities for {}", AQuietPlace.MOD_ID);
    }
}