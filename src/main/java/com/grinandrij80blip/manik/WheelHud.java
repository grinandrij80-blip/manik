package com.grinandrij80blip.manik;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;

import java.util.Random;

/** Circus wheel of fortune shown to a survivor hit by the clown's gift box: poison or blindness. */
public final class WheelHud {
    private static final int SPIN_TICKS = ClownManager.WHEEL_TICKS;
    private static final int HOLD_TICKS = 28;
    private static final int WEDGES = 8;

    private static long startNanos;
    private static boolean running;
    private static int result;      // 1 poison, 0 blindness
    private static int wedge;       // landing wedge

    public static void register() {
        AddGuiOverlayLayersEvent.BUS.addListener(event ->
                event.getLayeredDraw().add(Identifier.parse("manik:wheel_hud"), WheelHud::render));
    }

    public static void start(int res) {
        result = res;
        // even wedges are poison, odd wedges are blindness
        wedge = new Random().nextInt(WEDGES / 2) * 2 + (res == 1 ? 0 : 1);
        startNanos = System.nanoTime();
        running = true;
    }

    private static int color(int rgb, float a) {
        return (Math.max(0, Math.min(255, Math.round(a * 255.0F))) << 24) | (rgb & 0xFFFFFF);
    }

    private static void render(GuiGraphics g, net.minecraft.client.DeltaTracker deltaTracker) {
        if (!running) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) { running = false; return; }
        float t = (System.nanoTime() - startNanos) / 50_000_000.0F;
        if (t > SPIN_TICKS + HOLD_TICKS) { running = false; return; }

        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        float fadeIn = Math.min(1.0F, t / 6.0F);
        float fadeOut = Math.min(1.0F, (SPIN_TICKS + HOLD_TICKS - t) / 8.0F);
        float alpha = Math.min(fadeIn, fadeOut);
        g.fill(0, 0, w, h, color(0x000000, 0.45F * alpha));

        int cx = w / 2, cy = h / 2 + 6;
        int radius = Math.max(30, Math.min(70, h / 5)) ;
        radius = Math.round(radius * (0.6F + 0.4F * fadeIn));

        float p = Math.min(1.0F, t / SPIN_TICKS);
        float ease = 1.0F - (1.0F - p) * (1.0F - p) * (1.0F - p);
        float rotFinal = -90.0F - (wedge + 0.5F) * (360.0F / WEDGES);
        float rot = rotFinal - (1.0F - ease) * 360.0F * 4.0F;

        // wheel body, one run of equal colour per scanline stretch
        for (int dy = -radius; dy <= radius; dy++) {
            int half = (int) Math.floor(Math.sqrt((double) radius * radius - (double) dy * dy));
            int runStart = -half;
            int runColor = wedgeColor(-half, dy, rot, radius);
            for (int dx = -half + 1; dx <= half + 1; dx++) {
                int col = dx <= half ? wedgeColor(dx, dy, rot, radius) : -1;
                if (col != runColor) {
                    g.fill(cx + runStart, cy + dy, cx + dx, cy + dy + 1, color(runColor, alpha));
                    runStart = dx;
                    runColor = col;
                }
            }
        }
        // hub
        int hub = Math.max(3, radius / 7);
        g.fill(cx - hub, cy - hub, cx + hub, cy + hub, color(0xF2C230, alpha));
        g.fill(cx - hub + 2, cy - hub + 2, cx + hub - 2, cy + hub - 2, color(0xB08418, alpha));

        // circus bulbs around the rim
        int bulbs = 24;
        for (int i = 0; i < bulbs; i++) {
            double ang = i * Math.PI * 2.0 / bulbs;
            int bx = cx + (int) Math.round(Math.cos(ang) * (radius + 7));
            int by = cy + (int) Math.round(Math.sin(ang) * (radius + 7));
            boolean on = ((int) (t / 3.0F) + i) % 2 == 0;
            g.fill(bx - 2, by - 2, bx + 2, by + 2, color(on ? 0xFFE070 : 0x8A6A20, alpha));
        }

        // pointer at the top
        int pw = 7;
        for (int i = 0; i < 10; i++) {
            int half = Math.max(0, pw - i);
            g.fill(cx - half, cy - radius - 12 + i, cx + half + 1, cy - radius - 11 + i, color(0xFFFFFF, alpha));
        }

        String title = "КОЛЕСО ФОРТУНИ";
        float scale = 2.0F;
        g.pose().pushMatrix();
        g.pose().translate(w / 2, cy - radius - 34);
        g.pose().scale(scale, scale);
        g.drawString(mc.font, title, -mc.font.width(title) / 2, -4, color(0xF2C230, alpha));
        g.pose().popMatrix();

        if (t >= SPIN_TICKS) {
            String label = result == 1 ? "ОТРУТА!" : "СЛІПОТА!";
            g.pose().pushMatrix();
            g.pose().translate(w / 2, cy + radius + 26);
            g.pose().scale(2.5F, 2.5F);
            g.drawString(mc.font, label, -mc.font.width(label) / 2, -4,
                    color(result == 1 ? 0x62E04A : 0xB070FF, alpha));
            g.pose().popMatrix();
        }
    }

    /** Colour of the wheel at an offset from the centre; wedges alternate poison/blindness, with rim and stripes. */
    private static int wedgeColor(int dx, int dy, float rot, int radius) {
        double dist = Math.sqrt((double) dx * dx + (double) dy * dy);
        if (dist > radius - 2) return 0xF2C230;
        double theta = Math.toDegrees(Math.atan2(dy, dx)) - rot;
        theta = ((theta % 360.0) + 360.0) % 360.0;
        int k = (int) (theta / (360.0 / WEDGES));
        double within = theta - k * (360.0 / WEDGES);
        if (within < 1.2 || within > (360.0 / WEDGES) - 1.2) return 0xF2F2F2;
        boolean poison = k % 2 == 0;
        boolean inner = dist < radius * 0.55;
        if (poison) return inner ? 0x3FA830 : 0x62E04A;
        return inner ? 0x5A2A9A : 0x8A48E0;
    }

    private WheelHud() {}
}
