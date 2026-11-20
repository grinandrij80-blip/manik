package com.grinandrij80blip.manik;

import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A single-use 50% fuel container. Durability damage represents fuel already poured out.
 */
public final class CanisterItem extends Item {
    public static final int CAPACITY = 50;

    public CanisterItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int max = Math.max(1, stack.getMaxDamage());
        int remaining = Math.max(0, max - stack.getDamageValue());
        return Math.round(13.0F * remaining / max);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        int max = Math.max(1, stack.getMaxDamage());
        float remaining = (float) Math.max(0, max - stack.getDamageValue()) / max;
        return Mth.hsvToRgb(remaining / 3.0F, 1.0F, 1.0F);
    }
}
