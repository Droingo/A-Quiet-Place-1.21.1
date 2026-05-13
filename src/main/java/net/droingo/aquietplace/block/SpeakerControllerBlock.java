package net.droingo.aquietplace.block;

import com.mojang.serialization.MapCodec;
import net.droingo.aquietplace.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

public class SpeakerControllerBlock extends Block {
    public static final MapCodec<SpeakerControllerBlock> CODEC = createCodec(SpeakerControllerBlock::new);
    public static final BooleanProperty OPEN = BooleanProperty.of("open");

    private static final int AUTO_CLOSE_TICKS = 40;
    private static final int MIN_POLES = 1;
    private static final int MAX_POLES = 10;

    private static final VoxelShape SHAPE = VoxelShapes.union(
            Block.createCuboidShape(7.0, 0.0, 7.0, 9.0, 16.0, 9.0),
            Block.createCuboidShape(3.0, 2.0, 3.0, 7.0, 8.0, 7.0),
            Block.createCuboidShape(3.5, 5.0, 6.0, 5.0, 11.0, 8.0)
    );

    public SpeakerControllerBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(OPEN, false));
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        if (!(world instanceof ServerWorld serverWorld)) {
            return ActionResult.SUCCESS;
        }

        if (!state.get(OPEN)) {
            serverWorld.setBlockState(pos, state.with(OPEN, true), Block.NOTIFY_ALL);
        }

        serverWorld.scheduleBlockTick(pos, this, AUTO_CLOSE_TICKS);

        StructureCheckResult result = checkStructure(serverWorld, pos);

        player.sendMessage(
                Text.literal(result.message()),
                true
        );

        return ActionResult.SUCCESS;
    }

    @Override
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, net.minecraft.util.math.random.Random random) {
        if (state.get(OPEN)) {
            world.setBlockState(pos, state.with(OPEN, false), Block.NOTIFY_ALL);
        }
    }

    private StructureCheckResult checkStructure(ServerWorld world, BlockPos controllerPos) {
        int poles = 0;

        for (int offsetY = 1; offsetY <= MAX_POLES + 1; offsetY++) {
            BlockPos checkPos = controllerPos.up(offsetY);
            BlockState checkState = world.getBlockState(checkPos);

            if (checkState.isOf(ModBlocks.SPEAKER_POLE)) {
                poles++;
                continue;
            }

            if (checkState.isOf(ModBlocks.STUN_TRAP)) {
                if (poles < MIN_POLES) {
                    return StructureCheckResult.invalid("Structure invalid: add at least 1 Speaker Pole.");
                }

                return StructureCheckResult.valid(
                        "Structure valid. Poles: " + poles
                );
            }

            if (poles == 0) {
                return StructureCheckResult.invalid("Structure invalid: place Speaker Pole above controller.");
            }

            return StructureCheckResult.invalid("Structure invalid: missing Stun Trap above poles.");
        }

        return StructureCheckResult.invalid("Structure invalid: too many Speaker Poles. Max: " + MAX_POLES);
    }

    private record StructureCheckResult(boolean valid, String message) {
        private static StructureCheckResult valid(String message) {
            return new StructureCheckResult(true, message);
        }

        private static StructureCheckResult invalid(String message) {
            return new StructureCheckResult(false, message);
        }
    }
}