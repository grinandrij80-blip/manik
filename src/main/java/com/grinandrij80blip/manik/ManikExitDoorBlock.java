package com.grinandrij80blip.manik;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The MANIK exit panel becomes passable when every active generator is ready.
 * It continuously checks the current round and closes again after /game stop.
 */
public final class ManikExitDoorBlock extends Block {
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    private static final VoxelShape CLOSED_SHAPE = Block.box(0.0, 0.0, 7.0, 16.0, 16.0, 9.0);

    public ManikExitDoorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !state.is(oldState.getBlock())) {
            level.scheduleTick(pos, this, 5);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean shouldOpen = GameCommands.areAllGeneratorsReady(level);
        if (state.getValue(OPEN) != shouldOpen) {
            level.setBlock(pos, state.setValue(OPEN, shouldOpen), Block.UPDATE_CLIENTS);
        }
        level.scheduleTick(pos, this, 5);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPEN) ? Shapes.empty() : CLOSED_SHAPE;
    }
}
