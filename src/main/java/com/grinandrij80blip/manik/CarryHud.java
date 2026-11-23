package com.grinandrij80blip.manik;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;

import java.util.Random;

/**
 * Hint line while carrying the flare crate (the crate and arms themselves are drawn by CarryRender).
 */
public final class CarryHud {
    private static final Random RANDOM = new Random();

    public static void register() {
        AddGuiOverlayLayersEvent.BUS.addListener(event ->
                event.getLayeredDraw().add(Identifier.parse("manik:carry_hud"), CarryHud::render));
    }

    @SuppressWarnings("unused")
    private static void box(GuiGraphics g, float x0, float y0, float x1, float y1, int color) {
        g.fill(Math.round(x0), Math.round(y0), Math.round(x1), Math.round(y1), color);
    }

    private static void render(GuiGraphics g, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!CarryState.isCarrying() || minecraft.player == null || minecraft.options.hideGui) return;

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();

        String hint = "ПКМ у зоні виходу: запустити ракети  |  ПКМ: поставити  |  Shift+ПКМ: поставити в зоні";
        int tw = minecraft.font.width(hint);
        g.drawString(minecraft.font, hint, (width - tw) / 2, height - 60, 0xCCFFFFFF);
    }

    private CarryHud() {}
}
