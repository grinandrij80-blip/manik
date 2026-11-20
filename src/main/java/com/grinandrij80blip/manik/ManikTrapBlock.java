package com.grinandrij80blip.manik;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Three-dimensional bear trap with raised metal teeth.
 * The visible spikes do not create tall collision walls; the low base still
 * triggers stepOn reliably as players walk across it.
 */
public final class ManikTrapBlock extends Block {
    private static final VoxelShape COLLISION_SHAPE =
            Block.box(1.0, 0.0, 1.0, 15.0, 3.0, 15.0);

    private static final VoxelShape VISUAL_SHAPE = createVisualShape();

    private static VoxelShape createVisualShape() {
        VoxelShape shape = Block.box(1, 0, 1, 15, 2, 15);
        shape = Shapes.or(shape, Block.box(2, 2, 1, 14, 4, 3));
        shape = Shapes.or(shape, Block.box(2, 2, 13, 14, 4, 15));
        shape = Shapes.or(shape, Block.box(1, 2, 3, 3, 4, 13));
        shape = Shapes.or(shape, Block.box(13, 2, 3, 15, 4, 13));

        // Raised teeth around the inner edge of the jaws.
        shape = Shapes.or(shape, Block.box(5, 3, 3, 7, 9, 5));
        shape = Shapes.or(shape, Block.box(9, 3, 3, 11, 9, 5));
        shape = Shapes.or(shape, Block.box(5, 3, 11, 7, 9, 13));
        shape = Shapes.or(shape, Block.box(9, 3, 11, 11, 9, 13));
        shape = Shapes.or(shape, Block.box(3, 3, 5, 5, 9, 7));
        shape = Shapes.or(shape, Block.box(3, 3, 9, 5, 9, 11));
        shape = Shapes.or(shape, Block.box(11, 3, 5, 13, 9, 7));
        shape = Shapes.or(shape, Block.box(11, 3, 9, 13, 9, 11));
        return shape;
    }

    public ManikTrapBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (!level.isClientSide()) {
            TrapManager.onTrapStepped(level, pos, entity);
        }
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return COLLISION_SHAPE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return VISUAL_SHAPE;
    }
}
