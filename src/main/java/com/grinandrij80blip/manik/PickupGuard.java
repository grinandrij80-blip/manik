package com.grinandrij80blip.manik;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;

/** Maniacs cannot pick up map items during a round (only their own glove and trap). */
public final class PickupGuard {
    /** Returns true to cancel the pickup. */
    public static boolean onPickup(EntityItemPickupEvent event) {
        Player player = event.getEntity();
        if (!GameConfig.isRunning() || !player.getTags().contains("manik_killer")) return false;

        ItemStack stack = event.getItem().getItem();
        boolean ownRoleItem = stack.is(ManikItems.MARK_GLOVE.get()) || stack.is(ManikBlocks.TRAP_ITEM.get())
                || stack.is(ManikItems.TRIPWIRE.get()) || stack.is(ManikItems.CLOWN_HAMMER.get());
        return !ownRoleItem;
    }

    private PickupGuard() {}
}
