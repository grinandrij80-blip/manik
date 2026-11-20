package com.grinandrij80blip.manik;

import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;

/**
 * Client-only polling of vanilla key bindings. The server still validates
 * crouching, target block, item and generator state before applying progress.
 */
public final class ClientInputHandler {
    private static boolean lastSentHeld;

    public static void register() {
        TickEvent.ClientTickEvent.Pre.BUS.addListener(ClientInputHandler::onClientTickPre);
        TickEvent.ClientTickEvent.Post.BUS.addListener(ClientInputHandler::onClientTick);
    }

    private static void onClientTickPre(TickEvent.ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        StaminaHud.tickClient();
        suppressJumpWhileBlocked(minecraft);
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

        suppressJumpWhileBlocked(minecraft);

        boolean held = minecraft.screen == null
                && minecraft.options.keyShift.isDown()
                && minecraft.options.keyUse.isDown();

        if (held != lastSentHeld) {
            ManikNetwork.sendUseInput(held);
            lastSentHeld = held;
        }
    }

    private static void suppressJumpWhileBlocked(Minecraft minecraft) {
        if (minecraft.player == null || !StaminaHud.isJumpBlocked()) return;

        minecraft.options.keyJump.setDown(false);
        while (minecraft.options.keyJump.consumeClick()) {
            // Drain buffered jump presses so they cannot fire after the lock ends.
        }
    }

    private ClientInputHandler() {
    }
}
