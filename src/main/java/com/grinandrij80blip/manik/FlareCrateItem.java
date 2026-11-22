package com.grinandrij80blip.manik;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * The flare crate as an item. In creative mode it places the block (for building the map);
 * in survival, right click launches the flares when standing in an exit zone.
 */
public final class FlareCrateItem extends BlockItem {
    public FlareCrateItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || player.getAbilities().instabuild) return super.useOn(context);
        return launch(context.getLevel(), player, context);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.getAbilities().instabuild) return super.use(level, player, hand);
        return launch(level, player, null);
    }

    private InteractionResult launch(Level level, Player player, UseOnContext context) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp) {
            String error = ExitManager.launchFlares(sp);
            if (error != null) {
                sp.displayClientMessage(Component.literal(error), true);
            } else {
                net.minecraft.world.item.ItemStack held = sp.getItemInHand(
                        sp.getMainHandItem().is(this) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
                held.shrink(1);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
