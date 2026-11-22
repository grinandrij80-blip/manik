package com.grinandrij80blip.manik;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Special lever: pulling it starts the 60 second countdown to the exit opening. */
public final class ManikLeverBlock extends Block {
    public ManikLeverBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer serverPlayer) {
            String error = ExitManager.startCountdown(serverPlayer);
            if (error != null) {
                serverPlayer.displayClientMessage(Component.literal(error), true);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
