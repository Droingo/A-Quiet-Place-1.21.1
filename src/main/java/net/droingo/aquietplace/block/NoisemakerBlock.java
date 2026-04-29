package net.droingo.aquietplace.block;

import com.mojang.serialization.MapCodec;
import net.droingo.aquietplace.block.entity.NoisemakerBlockEntity;
import net.droingo.aquietplace.network.NoisemakerOpenScreenPayload;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.jetbrains.annotations.Nullable;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.droingo.aquietplace.registry.ModBlocks;
import net.minecraft.item.ItemStack;
import net.minecraft.state.property.BooleanProperty;
import net.droingo.aquietplace.registry.ModBlockEntities;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.state.property.BooleanProperty;

public class NoisemakerBlock extends BlockWithEntity {
    public static final MapCodec<NoisemakerBlock> CODEC = createCodec(NoisemakerBlock::new);
    public static final BooleanProperty ARMED = BooleanProperty.of("armed");
    public static final BooleanProperty ACTIVE = BooleanProperty.of("active");
    public static final net.minecraft.state.property.DirectionProperty FACING = HorizontalFacingBlock.FACING;
    public static final BooleanProperty BLINK = BooleanProperty.of("blink");

    private static final VoxelShape NORTH_SHAPE = Block.createCuboidShape(
            3.0, 0.0, 2.0,
            13.0, 8.0, 14.0
    );

    private static final VoxelShape SOUTH_SHAPE = Block.createCuboidShape(
            3.0, 0.0, 2.0,
            13.0, 8.0, 14.0
    );

    private static final VoxelShape EAST_SHAPE = Block.createCuboidShape(
            2.0, 0.0, 3.0,
            14.0, 8.0, 13.0
    );

    private static final VoxelShape WEST_SHAPE = Block.createCuboidShape(
            2.0, 0.0, 3.0,
            14.0, 8.0, 13.0
    );
    private static void pickupNoisemaker(World world, BlockPos pos, ServerPlayerEntity player) {
        ItemStack noisemakerStack = new ItemStack(ModBlocks.NOISEMAKER.asItem());

        /*
         * Remove the block without normal drops.
         * We are manually giving the item to the player.
         */
        world.breakBlock(pos, false, player);

        boolean inserted = player.getInventory().insertStack(noisemakerStack);

        if (!inserted) {
            player.dropItem(noisemakerStack, false);
        }
    }

    public NoisemakerBlock(AbstractBlock.Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(ARMED, false)
                .with(ACTIVE, false)
                .with(BLINK, false)
        );

    }
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world,
            BlockState state,
            BlockEntityType<T> type
    ) {
        if (world.isClient()) {
            return null;
        }

        if (type != ModBlockEntities.NOISEMAKER) {
            return null;
        }

        return (tickerWorld, pos, tickerState, blockEntity) -> {
            if (tickerWorld instanceof ServerWorld serverWorld
                    && blockEntity instanceof NoisemakerBlockEntity noisemaker) {
                NoisemakerBlockEntity.tick(serverWorld, pos, tickerState, noisemaker);
            }
        };
    }
    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }
    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
    @Override
    protected VoxelShape getOutlineShape(
            BlockState state,
            BlockView world,
            BlockPos pos,
            ShapeContext context
    ) {
        return getShapeForFacing(state);
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockView world,
            BlockPos pos,
            ShapeContext context
    ) {
        return getShapeForFacing(state);
    }

    private static VoxelShape getShapeForFacing(BlockState state) {
        Direction facing = state.get(FACING);

        return switch (facing) {
            case EAST -> EAST_SHAPE;
            case WEST -> WEST_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case NORTH -> NORTH_SHAPE;
            default -> NORTH_SHAPE;
        };
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        Direction playerFacing = context.getHorizontalPlayerFacing().getOpposite();
        return this.getDefaultState()
                .with(FACING, playerFacing)
                .with(ARMED, false)
                .with(ACTIVE, false);
    }

    @Override
    protected void appendProperties(StateManager.Builder<net.minecraft.block.Block, BlockState> builder) {
        builder.add(FACING, ARMED, ACTIVE, BLINK);
    }


    @Override
    protected ActionResult onUse(
            BlockState state,
            World world,
            BlockPos pos,
            PlayerEntity player,
            BlockHitResult hit
    ) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        if (!(world instanceof ServerWorld)) {
            return ActionResult.PASS;
        }

        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.PASS;
        }

        /*
         * Shift-right-click pickup:
         * This is useful for traps because admins/players can quickly reclaim them
         * without needing to break the block manually.
         */
        if (player.isSneaking()) {
            pickupNoisemaker(world, pos, serverPlayer);
            return ActionResult.SUCCESS;
        }

        if (!(world.getBlockEntity(pos) instanceof NoisemakerBlockEntity noisemaker)) {
            return ActionResult.PASS;
        }

        ServerPlayNetworking.send(serverPlayer, new NoisemakerOpenScreenPayload(
                pos,
                noisemaker.getDelaySeconds(),
                noisemaker.getRadius(),
                noisemaker.getStrength(),
                noisemaker.isArmed()
        ));

        return ActionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new NoisemakerBlockEntity(pos, state);
    }
}