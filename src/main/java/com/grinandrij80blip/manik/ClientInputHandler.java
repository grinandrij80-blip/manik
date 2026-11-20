package com.grinandrij80blip.manik;

import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;

/**
 * Client-only polling of the vanilla key bindings. The server still validates
 * crouching, target block, item and generator state before applying progress.
 */
public final class ClientInputHandler {
    private static boolean lastSentHeld;

    public static void register() {
        TickEvent.ClientTickEvent.Post.BUS.addListener(ClientInputHandler::onClientTick);
    }

    private static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean connected = minecraft.player != null
                && minecraft.level != null
                && minecraft.getConnection() != null;

        if (!connected) {
            lastSentHeld = false;
            return;
        }

        boolean held = minecraft.screen == null
                && minecraft.options.keyShift.isDown()
                && minecraft.options.keyUse.isDown();

        if (held != lastSentHeld) {
            ManikNetwork.sendUseInput(held);
            lastSentHeld = held;
        }
    }

    private ClientInputHandler() {
    }
}
