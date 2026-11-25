package com.grinandrij80blip.manik;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A hand that barely sticks out of the ground; it reaches up when a survivor comes close. */
public final class ManikHandTrapBlock extends Block {
    public static final BooleanProperty EXTENDED = BooleanProperty.create("extended");
    private static final VoxelShape SMALL = Block.box(5, 0, 5, 11, 3, 11);
    private static final VoxelShape BIG = Block.box(3, 0, 4, 12, 16, 12);

    public ManikHandTrapBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(EXTENDED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(EXTENDED);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return state.getValue(EXTENDED) ? BIG : SMALL;
    }
}
