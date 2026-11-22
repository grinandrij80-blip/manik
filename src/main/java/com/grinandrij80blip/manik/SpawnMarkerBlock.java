package com.grinandrij80blip.manik;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Map-building marker (item / generator / survivor / killer spawn). It works like the
 * vanilla light block: it is not rendered, has no collision (everyone walks through it)
 * and cannot be targeted, until the player holds one of the marker items. Then the
 * marker shows up as a particle icon and gets a selection outline.
 */
public final class SpawnMarkerBlock extends Block {
    public SpawnMarkerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return holdsMarkerItem(context) ? Shapes.block() : Shapes.empty();
    }

    private static boolean holdsMarkerItem(CollisionContext context) {
        return context.isHoldingItem(ManikBlocks.ITEM_SPAWN_ITEM.get())
                || context.isHoldingItem(ManikBlocks.GENERATOR_SPAWN_ITEM.get())
                || context.isHoldingItem(ManikBlocks.SURVIVOR_SPAWN_ITEM.get())
                || context.isHoldingItem(ManikBlocks.KILLER_SPAWN_ITEM.get())
                || context.isHoldingItem(ManikBlocks.EXIT_SPAWN_ITEM.get());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (level.isClientSide() && SpawnMarkerClient.isHoldingMarker()) {
            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK_MARKER, state),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0.0, 0.0, 0.0);
        }
    }
}
