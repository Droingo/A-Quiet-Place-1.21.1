package net.droingo.aquietplace.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public class WaterfallNoiseBlock extends Block {
    public WaterfallNoiseBlock(Settings settings) {
        super(settings);
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

    @Override
    protected VoxelShape getOutlineShape(
            BlockState state,
            BlockView world,
            BlockPos pos,
            ShapeContext context
    ) {
        return VoxelShapes.fullCube();
    }

    }