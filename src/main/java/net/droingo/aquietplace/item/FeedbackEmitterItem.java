package net.droingo.aquietplace.item;

import net.droingo.aquietplace.config.QuietPlaceConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class FeedbackEmitterItem extends Item {
    public FeedbackEmitterItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient()) {
            return TypedActionResult.success(stack);
        }

        if (!(world instanceof ServerWorld serverWorld)) {
            return TypedActionResult.pass(stack);
        }

        QuietPlaceConfig.FeedbackEmitter config = QuietPlaceConfig.get().feedbackEmitter;

        if (!config.enabled) {
            user.sendMessage(Text.literal("The Feedback Emitter is disabled."), true);
            return TypedActionResult.fail(stack);
        }

        FeedbackEmitterPulseManager.start(serverWorld, user, hand, config);

        user.getItemCooldownManager().set(this, 240);

        user.sendMessage(
                Text.literal("Feedback Emitter overloading..."),
                true
        );

        return TypedActionResult.success(stack);
    }
}