package net.droingo.aquietplace.item;

import net.droingo.aquietplace.registry.ModItems;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class EmptySandBagItem extends Item {
    public EmptySandBagItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos clickedPos = context.getBlockPos();
        BlockState clickedState = world.getBlockState(clickedPos);
        ItemStack emptyBagStack = context.getStack();
        PlayerEntity player = context.getPlayer();

        if (!isSandFillSource(clickedState)) {
            return ActionResult.PASS;
        }

        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        if (!(world instanceof ServerWorld serverWorld) || player == null) {
            return ActionResult.PASS;
        }

        if (!serverWorld.breakBlock(clickedPos, false, player)) {
            return ActionResult.PASS;
        }

        ItemStack filledBagStack = new ItemStack(ModItems.SAND_BAG);
        SandBagItem.fill(filledBagStack);

        if (!player.getAbilities().creativeMode) {
            if (emptyBagStack.getCount() == 1) {
                player.setStackInHand(context.getHand(), filledBagStack);
            } else {
                emptyBagStack.decrement(1);

                if (!player.getInventory().insertStack(filledBagStack)) {
                    player.dropItem(filledBagStack, false);
                }
            }
        } else {
            if (!player.getInventory().insertStack(filledBagStack)) {
                player.dropItem(filledBagStack, false);
            }
        }

        serverWorld.playSound(
                null,
                clickedPos,
                SoundEvents.BLOCK_SAND_BREAK,
                SoundCategory.BLOCKS,
                0.8f,
                0.85f
        );

        player.sendMessage(Text.literal("Sand Bag filled. Uses: " + SandBagItem.MAX_SAND_USES), true);

        return ActionResult.SUCCESS;
    }

    private static boolean isSandFillSource(BlockState state) {
        return state.isOf(Blocks.SAND) || state.isOf(Blocks.RED_SAND);
    }
}