package net.droingo.aquietplace.block;

import com.mojang.serialization.MapCodec;
import net.droingo.aquietplace.block.entity.GlassBottleTrapBlockEntity;
import net.droingo.aquietplace.registry.ModBlockEntities;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class GlassBottleTrapBlock extends BlockWithEntity {
    public static final MapCodec<GlassBottleTrapBlock> CODEC = createCodec(GlassBottleTrapBlock::new);

    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
    public static final IntProperty TRIGGER_STAGE = IntProperty.of("trigger_stage", 0, 2);

    /*
     * Small selection outline. Collision is empty below, so entities can walk over it.
     */
    private static final VoxelShape OUTLINE_SHAPE = Block.createCuboidShape(
            4.0, 0.0, 4.0,
            12.0, 9.0, 12.0
    );

    public GlassBottleTrapBlock(AbstractBlock.Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(TRIGGER_STAGE, 0)
        );
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        Direction playerFacing = context.getHorizontalPlayerFacing().getOpposite();

        return this.getDefaultState()
                .with(FACING, playerFacing)
                .with(TRIGGER_STAGE, 0);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new GlassBottleTrapBlockEntity(pos, state);
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

        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.PASS;
        }

        if (!player.isSneaking()) {
            serverPlayer.sendMessage(
                    net.minecraft.text.Text.literal("Sneak-right-click to attempt disarming."),
                    true
            );
            return ActionResult.SUCCESS;
        }

        if (world.getBlockEntity(pos) instanceof GlassBottleTrapBlockEntity trap) {
            trap.handleDisarmInteraction(serverPlayer);
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world,
            BlockState state,
            BlockEntityType<T> type
    ) {
        if (type != ModBlockEntities.GLASS_BOTTLE_TRAP) {
            return null;
        }

        return (tickerWorld, pos, tickerState, blockEntity) -> {
            if (blockEntity instanceof GlassBottleTrapBlockEntity glassBottleTrap) {
                GlassBottleTrapBlockEntity.tick(tickerWorld, pos, tickerState, glassBottleTrap);
            }
        };
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, TRIGGER_STAGE);
    }

    @Override
    protected BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    protected VoxelShape getOutlineShape(
            BlockState state,
            BlockView world,
            BlockPos pos,
            ShapeContext context
    ) {
        return OUTLINE_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockView world,
            BlockPos pos,
            ShapeContext context
    ) {
        return VoxelShapes.empty();
    }
}