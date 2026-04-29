package net.droingo.aquietplace.registry;

import net.droingo.aquietplace.AQuietPlace;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class ModSounds {
    public static final SoundEvent NOISEMAKER_START = registerSound("block.noisemaker.start");
    public static final SoundEvent NOISEMAKER_BEEP = registerSound("block.noisemaker.beep");
    public static final SoundEvent NOISEMAKER_NOISE = registerSound("block.noisemaker.noise");
    public static final SoundEvent GLASS_BOTTLE_TRAP_TRIGGER_0 = registerSound("block.glass_bottle_trap.trigger_0");
    public static final SoundEvent GLASS_BOTTLE_TRAP_TRIGGER_1 = registerSound("block.glass_bottle_trap.trigger_1");

    private ModSounds() {
    }

    public static void register() {
        AQuietPlace.LOGGER.info("Registered sounds for {}", AQuietPlace.MOD_ID);
    }

    private static SoundEvent registerSound(String name) {
        Identifier id = Identifier.of(AQuietPlace.MOD_ID, name);

        return Registry.register(
                Registries.SOUND_EVENT,
                id,
                SoundEvent.of(id)
        );
    }
}