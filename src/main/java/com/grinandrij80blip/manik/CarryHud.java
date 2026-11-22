package com.grinandrij80blip.manik;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;

import java.util.Random;

/**
 * First-person view of carrying the flare crate: the crate in front of the camera held
 * by both arms, swaying heavily and bobbing harder while walking.
 */
public final class CarryHud {
    private static final Random RANDOM = new Random();

    public static void register() {
        AddGuiOverlayLayersEvent.BUS.addListener(event ->
                event.getLayeredDraw().add(Identifier.parse("manik:carry_hud"), CarryHud::render));
    }

    private static void box(GuiGraphics g, float x0, float y0, float x1, float y1, int color) {
        g.fill(Math.round(x0), Math.round(y0), Math.round(x1), Math.round(y1), color);
    }

    private static void render(GuiGraphics g, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!CarryState.isCarrying() || minecraft.player == null || minecraft.options.hideGui) return;

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        float u = height / 34.0F;

        double time = System.currentTimeMillis() / 50.0;
        float moving = Math.min(1.0F, minecraft.player.walkAnimation.speed());
        // Heavy load: slow sway always, harder bobbing when walking.
        float swayX = (float) Math.sin(time * 0.07) * 0.5F * u + (float) Math.sin(time * 0.3) * 0.5F * u * moving;
        float bobY = 0.9F * u + (float) Math.sin(time * 0.1) * 0.25F * u
                + Math.abs((float) Math.sin(time * 0.3)) * 0.9F * u * moving;
        float tremble = 0.12F * u;

        float cx = width / 2.0F + swayX;
        float bottom = height + 2.0F * u + bobY;   // the lower part of the crate is below the screen
        float left = cx - 9.5F * u;
        float right = cx + 9.5F * u;
        float top = bottom - 13.0F * u;

        int dark = 0xFF2A0B09;
        int red = 0xFF9A2A22;
        int lightRed = 0xFFBB4036;
        int strap = 0xFF3A3A42;

        // Crate: top face, front face, straps and a flare label.
        box(g, left + 1.2F * u, top - 2.2F * u, right - 1.2F * u, top, lightRed);
        box(g, left - 0.2F * u, top - 0.2F * u, right + 0.2F * u, bottom, dark);
        box(g, left, top, right, bottom, red);
        box(g, left + 2.5F * u, top, left + 3.7F * u, bottom, strap);
        box(g, right - 3.7F * u, top, right - 2.5F * u, bottom, strap);
        box(g, cx - 4.0F * u, top + 3.0F * u, cx + 4.0F * u, top + 7.0F * u, 0xFFF0C93C);
        box(g, cx - 3.4F * u, top + 3.6F * u, cx + 3.4F * u, top + 4.6F * u, 0xFF2A0B09);
        box(g, cx - 2.0F * u, top + 5.2F * u, cx + 2.0F * u, top + 6.2F * u, 0xFFD43A1E);

        // Arms: sleeves come up from the bottom corners, hands grip the sides.
        int sleeve = 0xFF3A5FA0;
        int sleeveDark = 0xFF2A4678;
        int skin = 0xFFE0B48C;
        for (int side = -1; side <= 1; side += 2) {
            float shake = (RANDOM.nextFloat() - 0.5F) * tremble;
            float edge = side < 0 ? left : right;
            float outer = edge + side * 3.2F * u + shake;
            float inner = edge - side * 0.6F * u + shake;
            float x0 = Math.min(outer, inner);
            float x1 = Math.max(outer, inner);
            box(g, x0, top + 4.0F * u, x1, height + 1, sleeveDark);
            box(g, x0 + (side < 0 ? 0 : 0.5F * u), top + 4.0F * u, x1 - (side < 0 ? 0.5F * u : 0), height + 1, sleeve);
            // Hand wrapped around the crate edge.
            float h0 = edge - 1.6F * u + shake;
            float h1 = edge + 1.6F * u + shake;
            box(g, h0, top + 3.0F * u, h1, top + 6.2F * u, skin);
            box(g, h0, top + 5.3F * u, h1, top + 6.2F * u, 0xFFC09468);
        }

        String hint = "ПКМ у зоні виходу: запустити ракети  |  ПКМ: поставити  |  Shift+ПКМ: поставити в зоні";
        int tw = minecraft.font.width(hint);
        g.drawString(minecraft.font, hint, (width - tw) / 2, height - 60, 0xCCFFFFFF);
    }

    private CarryHud() {}
}
