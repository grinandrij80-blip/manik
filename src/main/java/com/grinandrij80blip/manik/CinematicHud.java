package com.grinandrij80blip.manik;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;

/** Full-screen blackout, 3-2-1 countdown and framed role / winner banners. */
public final class CinematicHud {
    private static final int NONE = 0, START = 1, FADE_IN = 2, WIN = 3;
    private static int mode = NONE;
    private static int value;
    private static long startNanos;

    public static void register() {
        AddGuiOverlayLayersEvent.BUS.addListener(event ->
                event.getLayeredDraw().add(Identifier.parse("manik:cinematic_hud"), CinematicHud::render));
    }

    public static void handle(int type, int val) {
        Minecraft mc = Minecraft.getInstance();
        if (type == ManikNetwork.CINEMATIC_MENU) {
            mc.setScreen(new ManiacSelectScreen(val));
            return;
        }
        if (type == ManikNetwork.CINEMATIC_MENU_CLOSE) {
            if (mc.screen instanceof ManiacSelectScreen) mc.setScreen(null);
            return;
        }
        if (type == ManikNetwork.CINEMATIC_START && mc.screen instanceof ManiacSelectScreen) mc.setScreen(null);
        value = val;
        startNanos = System.nanoTime();
        switch (type) {
            case ManikNetwork.CINEMATIC_START -> mode = START;
            case ManikNetwork.CINEMATIC_FADE_IN -> mode = FADE_IN;
            case ManikNetwork.CINEMATIC_WIN -> mode = WIN;
            default -> mode = NONE;
        }
    }

    private static float smooth(float x) {
        x = Math.max(0.0F, Math.min(1.0F, x));
        return x * x * (3.0F - 2.0F * x);
    }

    private static int color(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    private static void render(GuiGraphics g, net.minecraft.client.DeltaTracker deltaTracker) {
        if (mode == NONE) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) { mode = NONE; return; }
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        float t = (System.nanoTime() - startNanos) / 50_000_000.0F; // game ticks

        float dark = 0.0F;
        float bannerT = -1.0F;      // ticks since the banner appeared
        float bannerLen = 0.0F;
        int digit = 0;

        if (mode == START) {
            dark = smooth(t / CinematicManager.START_TICKS);
            if (t < CinematicManager.START_TICKS) digit = 3 - (int) (t / 20.0F);
            else dark = 1.0F;
            if (t > 200.0F) { mode = NONE; return; } // safety: never stay black
        } else if (mode == FADE_IN) {
            dark = 1.0F - smooth(t / 20.0F);
            bannerT = t;
            bannerLen = 30.0F;
            if (t > 40.0F) { mode = NONE; return; }
        } else if (mode == WIN) {
            int f = CinematicManager.END_FADE_TICKS;
            if (t < f) dark = smooth(t / f);
            else dark = 1.0F - smooth((t - f) / f);
            if (t >= f) { bannerT = t - f; bannerLen = CinematicManager.END_BANNER_TICKS; }
            if (t > f + CinematicManager.END_BANNER_TICKS + 40) { mode = NONE; return; }
        }

        if (dark > 0.0F) g.fill(0, 0, w, h, color(0x000000, dark));

        if (mode == START && digit > 0) {
            float within = (t % 20.0F) / 20.0F;
            float pop = 1.0F + 0.35F * (1.0F - smooth(within * 3.0F));
            bigText(g, mc, String.valueOf(digit), w / 2, h / 2, 9.0F * pop, 0xF2F2F2, 1.0F - within * 0.35F);
        }

        if (bannerT >= 0.0F && bannerT <= bannerLen) {
            float alpha = Math.min(smooth(bannerT / 5.0F), smooth((bannerLen - bannerT) / 6.0F));
            if (mode == FADE_IN) {
                boolean maniac = value != 1;
                String role = value == 0 ? "ТИ — НЕКРОМАНТ" : value == 2 ? "ТИ — КЛОУН" : value == 3 ? "ТИ — БДЖОЛЯР" : "ТИ — ВИЖИВШИЙ";
                int accent = value == 2 ? 0xB0388C : value == 3 ? 0xE6B93A : maniac ? 0xC21B1B : 0x2FBF8F;
                banner(g, mc, w, h, role,
                        maniac ? "Знайди і спіймай усіх" : "Запусти генератори і втечи",
                        accent, maniac ? 0x1A0505 : 0x04160F, alpha);
            } else if (mode == WIN) {
                boolean maniacWon = value == CinematicManager.WINNER_MANIAC;
                banner(g, mc, w, h, maniacWon ? "МАНІЯК ПЕРЕМІГ" : "ВИЖИВШІ ПЕРЕМОГЛИ",
                        maniacWon ? "Ніхто не втік" : "Вертоліт забрав їх із цього місця",
                        maniacWon ? 0xC21B1B : 0xE6B93A, maniacWon ? 0x1A0505 : 0x16110A, alpha);
            }
        }
    }

    private static void bigText(GuiGraphics g, Minecraft mc, String text, int cx, int cy,
                                float scale, int rgb, float alpha) {
        int tw = mc.font.width(text);
        g.pose().pushMatrix();
        g.pose().translate(cx, cy);
        g.pose().scale(scale, scale);
        g.drawString(mc.font, text, -tw / 2, -4, color(rgb, alpha));
        g.pose().popMatrix();
    }

    /** Framed banner: dark panel, double border in the role colour, corner studs. */
    private static void banner(GuiGraphics g, Minecraft mc, int w, int h, String title, String sub,
                               int accent, int bg, float alpha) {
        float scale = Math.max(2.0F, Math.min(4.0F, w / 260.0F));
        int tw = (int) (mc.font.width(title) * scale);
        int sw = mc.font.width(sub);
        int panelW = Math.max(tw, sw * 2) + 70;
        int panelH = (int) (9 * scale) + 48;
        int x0 = (w - panelW) / 2, y0 = h / 2 - panelH / 2 - h / 12;
        int x1 = x0 + panelW, y1 = y0 + panelH;

        g.fill(x0, y0, x1, y1, color(bg, 0.82F * alpha));
        int line = color(accent, alpha);
        int dim = color(accent, 0.45F * alpha);
        // outer thick border
        g.fill(x0, y0, x1, y0 + 3, line);
        g.fill(x0, y1 - 3, x1, y1, line);
        g.fill(x0, y0, x0 + 3, y1, line);
        g.fill(x1 - 3, y0, x1, y1, line);
        // inner thin border
        g.fill(x0 + 7, y0 + 7, x1 - 7, y0 + 8, dim);
        g.fill(x0 + 7, y1 - 8, x1 - 7, y1 - 7, dim);
        g.fill(x0 + 7, y0 + 7, x0 + 8, y1 - 7, dim);
        g.fill(x1 - 8, y0 + 7, x1 - 7, y1 - 7, dim);
        // corner studs
        int[][] corners = {{x0, y0}, {x1, y0}, {x0, y1}, {x1, y1}};
        for (int[] c : corners) g.fill(c[0] - 4, c[1] - 4, c[0] + 4, c[1] + 4, line);
        // side wings
        int cy = (y0 + y1) / 2;
        g.fill(x0 - 26, cy - 1, x0 - 6, cy + 1, line);
        g.fill(x1 + 6, cy - 1, x1 + 26, cy + 1, line);

        bigText(g, mc, title, w / 2, y0 + 12 + (int) (4 * scale), scale, 0xFFFFFF, alpha);
        bigText(g, mc, sub, w / 2, y1 - 17, 1.6F, accent, alpha);
    }

    private CinematicHud() {}
}
