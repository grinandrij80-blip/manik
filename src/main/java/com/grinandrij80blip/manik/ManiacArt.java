package com.grinandrij80blip.manik;

import net.minecraft.client.gui.GuiGraphics;

/** Pixel-art portraits and icons for the maniac menu, power HUD and scares. */
public final class ManiacArt {
    private static int c(int argb, float a) {
        int al = Math.round(((argb >>> 24) & 0xFF) * a);
        return (al << 24) | (argb & 0xFFFFFF);
    }

    private static void box(GuiGraphics g, int cx, int cy, float u,
                            float x0, float y0, float x1, float y1, int color, float a) {
        g.fill(cx + Math.round(x0 * u), cy + Math.round(y0 * u),
                cx + Math.round(x1 * u), cy + Math.round(y1 * u), c(color, a));
    }

    /** Screaming hooded maniac with a pale mask. */
    public static void bruteFace(GuiGraphics g, int cx, int cy, float u, float mouth) {
        int dark = 0xFF0A0A0A;
        int pale = 0xFFE6E6DC;
        box(g, cx, cy, u, -6, -12, -4, -8, 0xFF3A0A0A, 1);
        box(g, cx, cy, u, 4, -12, 6, -8, 0xFF3A0A0A, 1);
        box(g, cx, cy, u, -8, -9, 9, 11, dark, 1);
        box(g, cx, cy, u, -6.5F, -8, 7.5F, 9.5F, pale, 1);
        box(g, cx, cy, u, -6.5F, -7, -4, -5.5F, dark, 1);
        box(g, cx, cy, u, -4, -5.5F, -1.5F, -4, dark, 1);
        box(g, cx, cy, u, -1.5F, -4, 0, -2.5F, dark, 1);
        box(g, cx, cy, u, 5, -7, 7.5F, -5.5F, dark, 1);
        box(g, cx, cy, u, 2.5F, -5.5F, 5, -4, dark, 1);
        box(g, cx, cy, u, 0.5F, -4, 2.5F, -2.5F, dark, 1);
        box(g, cx, cy, u, -5.5F, -3, -1, -0.5F, 0xFFFF1A1A, 1);
        box(g, cx, cy, u, 2, -3, 6.5F, -0.5F, 0xFFFF1A1A, 1);
        box(g, cx, cy, u, -3.2F, -2.2F, -2.8F, -1.0F, dark, 1);
        box(g, cx, cy, u, 3.8F, -2.2F, 4.2F, -1.0F, dark, 1);
        box(g, cx, cy, u, -1, 1, 2, 3.5F, 0xFF2A2A2A, 1);
        float mouthBottom = 4.5F + 5.0F * mouth;
        box(g, cx, cy, u, -4.5F, 4.5F, 5.5F, mouthBottom, dark, 1);
        box(g, cx, cy, u, -4, 4.5F, 5, 5.7F, 0xFFFFFFFF, 1);
        box(g, cx, cy, u, -4, mouthBottom - 1.2F, 5, mouthBottom, 0xFFFFFFFF, 1);
        box(g, cx, cy, u, -2.5F, mouthBottom - 2.8F, 3.5F, mouthBottom - 1.2F, 0xFF7A1010, 1);
    }

    /** Headless beekeeper: fat yellow suit with black stripes, a bloody neck stump and little wings. */
    public static void beeFace(GuiGraphics g, int cx, int cy, float u, float a) {
        int dark = 0xFF14100C;
        box(g, cx, cy, u, -9, -8, -4, -2, 0xFFB8D8F0, a);       // wings
        box(g, cx, cy, u, 4, -8, 9, -2, 0xFFB8D8F0, a);
        box(g, cx, cy, u, -7, -3, 7, 12, dark, a);
        box(g, cx, cy, u, -6, -2, 6, 11, 0xFFEBBE24, a);        // fat belly
        box(g, cx, cy, u, -6, 1, 6, 3, dark, a);
        box(g, cx, cy, u, -6, 6, 6, 8, dark, a);
        box(g, cx, cy, u, -3, -6, 3, -2, 0xFF3C3414, a);        // collar
        box(g, cx, cy, u, -2.5F, -7, 2.5F, -5, 0xFF8A0E0E, a);  // neck stump
        box(g, cx, cy, u, -0.5F, -2, 0.5F, 11, 0xFFA0A4AC, a);  // zipper
        box(g, cx, cy, u, -1.5F, -7.5F, -0.5F, -6.5F, 0xFFE0D8C8, a);
    }

    /** Fallen angel: pale scarred face and burnt wings. */
    public static void angelFace(GuiGraphics g, int cx, int cy, float u, float a) {
        int ash = 0xFF201C1E;
        box(g, cx, cy, u, -11, -10, -4, 6, ash, a);              // burnt wings
        box(g, cx, cy, u, 4, -10, 11, 6, 0xFFE0DCD0, a);
        box(g, cx, cy, u, 7, -10, 9, -6, ash, a);
        box(g, cx, cy, u, -4, -7, 4, 5, 0xFFC4B8B0, a);          // face
        box(g, cx, cy, u, -4, -7, 4, -5, 0xFF3A3234, a);         // scorched hair
        box(g, cx, cy, u, -3, -3, -1, -1, 0xFF0A0608, a);        // small hollow eye
        box(g, cx, cy, u, 0.5F, -4, 3.5F, -0.5F, 0xFFE8E2D6, a);  // big eye
        box(g, cx, cy, u, 1.5F, -3, 2.5F, -1.5F, 0xFF100606, a);
        box(g, cx, cy, u, -2.5F, 2, 2.5F, 2.6F, 0xFF1C0A0A, a); // stitched mouth
        box(g, cx, cy, u, 2, 0, 2.6F, 4, 0xFFA01414, a);         // blood tear
    }

    /** Gaunt clown with black eye makeup, red nose and a wide bloody grin. */
    public static void clownFace(GuiGraphics g, int cx, int cy, float u, float a) {
        int outline = 0xFF140A0A;
        int skin = 0xFFE8E4DA;
        int shadow = 0xFFB8B4AA;
        int red = 0xFFC81414;
        int dred = 0xFF5A0A0A;
        int hair = 0xFFD2561E;
        int hair2 = 0xFF9A3A12;
        for (int s = -1; s <= 1; s += 2) {
            box(g, cx, cy, u, s < 0 ? -12 : 7, -9, s < 0 ? -7 : 12, -3, hair, a);
            box(g, cx, cy, u, s < 0 ? -13 : 7.5F, -5, s < 0 ? -7.5F : 13, 1, hair2, a);
            box(g, cx, cy, u, s < 0 ? -11 : 7, -1, s < 0 ? -7 : 11, 3, hair, a);
        }
        box(g, cx, cy, u, -7.5F, -10, 7.5F, 11.5F, outline, a);
        box(g, cx, cy, u, -6.5F, -9, 6.5F, 10.5F, skin, a);
        box(g, cx, cy, u, -6.5F, 6, -5, 10.5F, shadow, a);
        box(g, cx, cy, u, 5, 6, 6.5F, 10.5F, shadow, a);
        for (int s = -1; s <= 1; s += 2) {
            float m = s;
            // angry brow
            box(g, cx, cy, u, m < 0 ? -6 : 2, -7.5F, m < 0 ? -2 : 6, -6.3F, outline, a);
            // diamond eye makeup
            box(g, cx, cy, u, m * 3.5F - 0.7F, -5.8F, m * 3.5F + 0.7F, -4.6F, outline, a);
            box(g, cx, cy, u, m * 3.5F - 1.7F, -4.6F, m * 3.5F + 1.7F, -2.6F, outline, a);
            box(g, cx, cy, u, m * 3.5F - 0.7F, -2.6F, m * 3.5F + 0.7F, -1.4F, outline, a);
            box(g, cx, cy, u, m * 3.5F - 0.5F, -4.2F, m * 3.5F + 0.5F, -3.0F, 0xFFE01818, a);
            // eye drip
            box(g, cx, cy, u, m * 4.2F - 0.2F, -1.4F, m * 4.2F + 0.2F, 3.0F, dred, a);
        }
        box(g, cx, cy, u, -1.7F, 0, 1.7F, 3.4F, 0xFFD01818, a);
        box(g, cx, cy, u, -0.9F, 0.6F, -0.1F, 1.4F, 0xFFFF7A6A, a);
        // wide grin with scars at the corners
        box(g, cx, cy, u, -7, 2.5F, -6, 4.2F, red, a);
        box(g, cx, cy, u, 6, 2.5F, 7, 4.2F, red, a);
        box(g, cx, cy, u, -6.5F, 3.4F, 6.5F, 4.4F, red, a);
        box(g, cx, cy, u, -6, 4.2F, 6, 7.6F, dred, a);
        for (int i = 0; i < 6; i++) {
            float x = -5.2F + i * 1.9F;
            box(g, cx, cy, u, x, 4.2F, x + 1.2F, 5.7F, 0xFFEDEBD0, a);
            box(g, cx, cy, u, x + 0.9F, 6.0F, x + 1.9F, 7.2F, 0xFFEDEBD0, a);
        }
        box(g, cx, cy, u, 1, 7.6F, 1.5F, 10.5F, dred, a);
        box(g, cx, cy, u, -3.5F, 7.6F, -3, 9.6F, dred, a);
    }

    /** Surprise box with ribbon and bow (power 2 of the clown). */
    public static void giftBox(GuiGraphics g, int cx, int cy, float u, boolean lit) {
        float a = lit ? 1.0F : 0.45F;
        box(g, cx, cy, u, -8.5F, -3, 8.5F, 9.5F, 0xFF140A0A, a);
        box(g, cx, cy, u, -8, -2.5F, 8, 9, 0xFFCC2222, a);
        box(g, cx, cy, u, -9, -7.5F, 9, -2, 0xFF140A0A, a);
        box(g, cx, cy, u, -8.5F, -7, 8.5F, -2.5F, 0xFFE03030, a);
        box(g, cx, cy, u, -1.5F, -7, 1.5F, 9, 0xFFF2C230, a);
        box(g, cx, cy, u, -8, 1, 8, 3.5F, 0xFFF2C230, a);
        box(g, cx, cy, u, -5.5F, -11.5F, -0.5F, -7, 0xFFF2C230, a);
        box(g, cx, cy, u, 0.5F, -11.5F, 5.5F, -7, 0xFFF2C230, a);
        box(g, cx, cy, u, -4.3F, -10.5F, -1.5F, -8, 0xFFB08418, a);
        box(g, cx, cy, u, 1.5F, -10.5F, 4.3F, -8, 0xFFB08418, a);
        box(g, cx, cy, u, -1, -9, 1, -6.5F, 0xFFD9A421, a);
    }

    private ManiacArt() {}
}
