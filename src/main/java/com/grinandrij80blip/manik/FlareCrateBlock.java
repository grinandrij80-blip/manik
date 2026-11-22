package com.grinandrij80blip.manik;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Flare crate placed in the room behind special door 2. A survivor right-clicks it to pick it up. */
public final class FlareCrateBlock extends Block {
    public FlareCrateBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        if (!GameConfig.isRunning()) {
            sp.displayClientMessage(Component.literal("Ящик можна брати лише під час раунду."), true);
            return InteractionResult.SUCCESS;
        }
        if (!sp.getTags().contains("manik_survivor")) {
            sp.displayClientMessage(Component.literal("Ящик сигнальних ракет можуть брати лише виживші."), true);
            return InteractionResult.SUCCESS;
        }

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        ItemStack crate = new ItemStack(ManikBlocks.FLARE_CRATE_ITEM.get());
        if (!sp.getInventory().add(crate)) sp.drop(crate, false);
        sp.displayClientMessage(Component.literal(
                "Ти взяв ящик сигнальних ракет. Віднеси його в зону виходу й натисни ПКМ."), true);
        return InteractionResult.SUCCESS;
    }
}
