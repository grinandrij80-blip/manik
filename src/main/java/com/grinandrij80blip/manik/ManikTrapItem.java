package com.grinandrij80blip.manik;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Reusable trap item. A successful placement starts its normal Minecraft
 * cooldown but restores the one item consumed by vanilla BlockItem placement.
 */
public final class ManikTrapItem extends BlockItem {
    public ManikTrapItem(ManikTrapBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        int countBefore = stack.getCount();

        InteractionResult result = super.useOn(context);

        if (result.consumesAction()
                && context.getPlayer() != null
                && !context.getPlayer().getAbilities().instabuild) {
            int consumed = countBefore - stack.getCount();
            if (consumed > 0) {
                stack.grow(consumed);
            }
        }

        return result;
    }
}
