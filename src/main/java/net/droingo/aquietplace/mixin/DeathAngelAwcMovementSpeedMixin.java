package net.droingo.aquietplace.mixin;

import net.droingo.aquietplace.entity.deathangel.DeathAngelEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = DeathAngelEntity.class, remap = false)
public abstract class DeathAngelAwcMovementSpeedMixin {
    public float getMovementSpeed() {
        return ((DeathAngelEntity) (Object) this).getAwcMovementSpeedValue();
    }
}