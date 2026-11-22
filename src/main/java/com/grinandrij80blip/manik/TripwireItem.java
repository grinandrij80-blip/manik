package com.grinandrij80blip.manik;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** Maniac tripwire: right click one surface, then another (up to 5 blocks away). */
public final class TripwireItem extends Item {
    public TripwireItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (context.getPlayer() instanceof ServerPlayer player) {
            TripwireManager.onUse(player, context.getItemInHand(),
                    context.getClickLocation(), context.getClickedFace());
        }
        return InteractionResult.SUCCESS;
    }
}
