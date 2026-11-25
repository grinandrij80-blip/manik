package com.grinandrij80blip.manik;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.event.TickEvent;

/** Client side of the hand trap: a right click while grabbed opens the escape mini-game. */
public final class HandTrapClient {
    private static boolean grabbed;
    private static boolean lastUse;

    public static void register() {
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) { grabbed = false; return; }
            boolean use = mc.options.keyUse.isDown();
            if (grabbed && use && !lastUse && mc.screen == null) {
                mc.setScreen(new HandMinigameScreen());
            }
            lastUse = use;
        });
        AddGuiOverlayLayersEvent.BUS.addListener(event ->
                event.getLayeredDraw().add(Identifier.parse("manik:hand_trap_hud"), HandTrapClient::render));
    }

    public static void setGrabbed(boolean value) {
        grabbed = value;
        Minecraft mc = Minecraft.getInstance();
        if (!value && mc.screen instanceof HandMinigameScreen) mc.setScreen(null);
    }

    private static void render(GuiGraphics g, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (!grabbed || mc.player == null || mc.screen != null || mc.options.hideGui) return;
        String text = "РУКА ТРИМАЄ ТЕБЕ — натисни ПКМ";
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        int tw = mc.font.width(text);
        g.fill((w - tw) / 2 - 6, h - 80, (w + tw) / 2 + 6, h - 66, 0xAA300808);
        g.drawString(mc.font, text, (w - tw) / 2, h - 77, 0xFFFF6A5A);
    }

    private HandTrapClient() {}
}
