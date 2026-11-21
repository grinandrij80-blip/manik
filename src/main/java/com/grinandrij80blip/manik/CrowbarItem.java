package com.grinandrij80blip.manik;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Three-use tool for survivors. It disarms MANIK traps (see TrapManager) and
 * stuns the maniac for a few seconds when used to hit them (see GloveManager).
 * Every successful use spends one of its three durability points.
 */
public final class CrowbarItem extends Item {
    public CrowbarItem(Properties properties) {
        super(properties);
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
