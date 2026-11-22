package com.grinandrij80blip.manik;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Three-use tool for survivors. It disarms MANIK traps (see TrapManager) and
 * stuns the maniac for a few seconds when used to hit them (see GloveManager).
 * Every successful use spends one of its three durability points.
 */
public final class CrowbarItem extends Item {
    public CrowbarItem(Properties properties) {
        super(properties);
    }

    /** A trapped survivor can free themselves by using the crowbar. */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && TrapManager.freeSelf(serverPlayer, hand, player.getItemInHand(hand))) {
            return InteractionResult.SUCCESS;
        }
        return super.use(level, player, hand);
    }

    /** Spends one use of the crowbar and removes it from the hand after the last one. */
    public static void consumeUse(Player player, InteractionHand hand, ItemStack crowbar) {
        int nextDamage = crowbar.getDamageValue() + 1;
        if (nextDamage >= crowbar.getMaxDamage()) {
            player.setItemInHand(hand, ItemStack.EMPTY);
        } else {
            crowbar.setDamageValue(nextDamage);
        }
    }
}
