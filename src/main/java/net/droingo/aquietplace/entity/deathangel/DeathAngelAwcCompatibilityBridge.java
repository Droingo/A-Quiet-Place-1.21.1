package net.droingo.aquietplace.entity.deathangel;

import com.nyfaria.awcapi.entity.IAdvancedClimber;
import net.minecraft.block.BlockState;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;

public interface DeathAngelAwcCompatibilityBridge extends IAdvancedClimber {
    @Override
    default float getMovementSpeed() {
        return (float) this.asMob().getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
    }

    @Override
    default float getBlockSlipperiness(BlockPos pos) {
        MobEntity mob = this.asMob();
        BlockState state = mob.getWorld().getBlockState(pos);
        return state.getBlock().getSlipperiness();
    }

    @Override
    default void setLerpYRot(Float yRot) {
        if (yRot != null) {
            this.asMob().setYaw(yRot);
        }
    }

    @Override
    default void setLerpXRot(Float xRot) {
        if (xRot != null) {
            this.asMob().setPitch(xRot);
        }
    }

    @Override
    default void setLerpYHeadRot(Float yHeadRot) {
        if (yHeadRot != null) {
            this.asMob().setHeadYaw(yHeadRot);
        }
    }

    @Override
    default void setLerpHeadSteps(int steps) {
        // Not needed for the Death Angel.
    }
}