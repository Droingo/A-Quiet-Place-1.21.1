package net.droingo.aquietplace.item;

import net.droingo.aquietplace.registry.ModBlocks;
import net.droingo.aquietplace.registry.ModItems;
import net.minecraft.block.BlockState;
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
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class SandBagItem extends Item {
    public static final int MAX_SAND_USES = 8;

    public SandBagItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos clickedPos = context.getBlockPos();
        BlockState clickedState = world.getBlockState(clickedPos);
        ItemStack stack = context.getStack();
        PlayerEntity player = context.getPlayer();

        if (isEmpty(stack)) {
            if (!world.isClient() && player != null) {
                player.sendMessage(Text.literal("The sand bag is empty."), true);
            }

            return ActionResult.PASS;
        }

        if (context.getSide() != Direction.UP) {
            return ActionResult.PASS;
        }

        if (!canSupportSandPathLayer(world, clickedPos, clickedState)) {
            if (!world.isClient() && player != null) {
                player.sendMessage(Text.literal("Sand can only be placed on solid block tops."), true);
            }

            return ActionResult.PASS;
        }

        BlockPos placementPos = clickedPos.up();

        if (!world.getBlockState(placementPos).isAir()) {
            return ActionResult.PASS;
        }

        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        if (!(world instanceof ServerWorld serverWorld)) {
            return ActionResult.PASS;
        }

        placeSandPathLayer(serverWorld, placementPos, stack, player, context);

        return ActionResult.SUCCESS;
    }

    private void placeSandPathLayer(
            ServerWorld world,
            BlockPos placementPos,
            ItemStack stack,
            PlayerEntity player,
            ItemUsageContext context
    ) {
        world.setBlockState(
                placementPos,
                ModBlocks.SAND_PATH_LAYER.getDefaultState()
        );

        world.playSound(
                null,
                placementPos,
                SoundEvents.BLOCK_SAND_PLACE,
                SoundCategory.BLOCKS,
                0.8f,
                1.0f
        );

        if (player == null || player.getAbilities().creativeMode) {
            return;
        }

        consumeUse(stack);

        int usesLeft = getUsesRemaining(stack);

        if (usesLeft <= 0) {
            player.setStackInHand(context.getHand(), new ItemStack(ModItems.EMPTY_SAND_BAG));
            player.sendMessage(Text.literal("Sand Bag emptied."), true);
            return;
        }

        player.sendMessage(Text.literal("Sand placed. Uses left: " + usesLeft), true);
    }

    private static boolean canSupportSandPathLayer(World world, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return false;
        }

        if (state.isOf(ModBlocks.SAND_PATH_LAYER)) {
            return false;
        }

        return state.isSideSolidFullSquare(world, pos, Direction.UP);
    }

    public static boolean isEmpty(ItemStack stack) {
        return stack.getDamage() >= stack.getMaxDamage();
    }

    public static int getUsesRemaining(ItemStack stack) {
        if (!stack.isDamageable()) {
            return 0;
        }

        return Math.max(0, stack.getMaxDamage() - stack.getDamage());
    }

    public static void fill(ItemStack stack) {
        stack.setDamage(0);
    }

    public static void consumeUse(ItemStack stack) {
        if (!stack.isDamageable()) {
            return;
        }

        stack.setDamage(Math.min(stack.getMaxDamage(), stack.getDamage() + 1));
    }
}