package com.grinandrij80blip.manik;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/** Client-only helper: is the local player holding a spawn marker in their main hand? */
public final class SpawnMarkerClient {
    public static boolean isHoldingMarker() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return false;
        ItemStack stack = minecraft.player.getMainHandItem();
        return stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof SpawnMarkerBlock;
    }

    private SpawnMarkerClient() {}
}
