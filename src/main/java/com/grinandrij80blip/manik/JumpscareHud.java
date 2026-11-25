package com.grinandrij80blip.manik;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.event.TickEvent;

import java.util.Random;

/** Animated screaming maniac face in the middle of the screen (survivors, when the maniac uses power 1). */
public final class JumpscareHud {
    private static final int BRUTE_TICKS = 29; // 35% shorter than the original 45
    private static final int CLOWN_TICKS = 10; // 0.5 s
    private static int TOTAL_TICKS = BRUTE_TICKS;
    private static int kind;
    private static final Random RANDOM = new Random();
    private static int age = BRUTE_TICKS;

    public static void register() {
        AddGuiOverlayLayersEvent.BUS.addListener(event ->
                event.getLayeredDraw().add(Identifier.parse("manik:jumpscare_hud"), JumpscareHud::render));
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> {
            if (age < TOTAL_TICKS) age++;
        });
    }

    public static void start(int scareKind) {
        kind = scareKind;
        TOTAL_TICKS = scareKind == 1 ? CLOWN_TICKS : BRUTE_TICKS;
        age = 0;
    }

    private static int px(int center, float units, float u) {
        return center + Math.round(units * u);
    }

    private static void box(GuiGraphics g, int cx, int cy, float u,
                            float x0, float y0, float x1, float y1, int color) {
        g.fill(px(cx, x0, u), px(cy, y0, u), px(cx, x1, u), px(cy, y1, u), color);
    }

    private static void render(GuiGraphics g, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (age >= TOTAL_TICKS || minecraft.player == null) return;

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();

        if (kind == 1) {
            float p = age / (float) TOTAL_TICKS;
            int flash = (int) (160 * (1.0F - p));
            g.fill(0, 0, width, height, (flash << 24) | 0x00A00000);
            float cu = height / 20.0F * (0.5F + 1.1F * Math.min(1.0F, p * 3.0F));
            int jx = Math.round((RANDOM.nextFloat() - 0.5F) * cu * 0.4F);
            int jy = Math.round((RANDOM.nextFloat() - 0.5F) * cu * 0.4F);
            ManiacArt.clownFace(g, width / 2 + jx, height / 2 + jy, cu, 1.0F);
            return;
        }

        // Red flash at the start.
        if (age < 14) {
            int alpha = (int) (170 * (1.0F - age / 14.0F));
            g.fill(0, 0, width, height, (alpha << 24) | 0x00B00000);
        }

        // Punch in from small to full size, then keep creeping closer.
        float punch = Math.min(1.0F, age / 5.0F);
        float u = height / 24.0F * (0.35F + 0.65F * punch + 0.15F * (age / (float) TOTAL_TICKS));
        float shake = age < 32 ? u * 0.18F : 0.0F;
        int cx = width / 2 + Math.round((RANDOM.nextFloat() - 0.5F) * shake);
        int cy = height / 2 + Math.round((RANDOM.nextFloat() - 0.5F) * shake);

        float mouth = 0.55F + 0.45F * (float) Math.abs(Math.sin(age * 0.9F));
        int dark = 0xFF0A0A0A;
        int pale = 0xFFE6E6DC;

        box(g, cx, cy, u, -6, -12, -4, -8, 0xFF3A0A0A);              // horns
        box(g, cx, cy, u, 4, -12, 6, -8, 0xFF3A0A0A);
        box(g, cx, cy, u, -8, -9, 9, 11, dark);                      // hood
        box(g, cx, cy, u, -6.5F, -8, 7.5F, 9.5F, pale);              // mask
        box(g, cx, cy, u, -6.5F, -7, -4, -5.5F, dark);               // angry brows
        box(g, cx, cy, u, -4, -5.5F, -1.5F, -4, dark);
        box(g, cx, cy, u, -1.5F, -4, 0, -2.5F, dark);
        box(g, cx, cy, u, 5, -7, 7.5F, -5.5F, dark);
        box(g, cx, cy, u, 2.5F, -5.5F, 5, -4, dark);
        box(g, cx, cy, u, 0.5F, -4, 2.5F, -2.5F, dark);
        box(g, cx, cy, u, -5.5F, -3, -1, -0.5F, 0xFFFF1A1A);         // glowing red eyes
        box(g, cx, cy, u, 2, -3, 6.5F, -0.5F, 0xFFFF1A1A);
        box(g, cx, cy, u, -3.2F, -2.2F, -2.8F, -1.0F, dark);         // pupils
        box(g, cx, cy, u, 3.8F, -2.2F, 4.2F, -1.0F, dark);
        box(g, cx, cy, u, -1, 1, 2, 3.5F, 0xFF2A2A2A);               // nose
        float mouthBottom = 4.5F + 5.0F * mouth;
        box(g, cx, cy, u, -4.5F, 4.5F, 5.5F, mouthBottom, dark);     // screaming mouth
        box(g, cx, cy, u, -4, 4.5F, 5, 5.7F, 0xFFFFFFFF);            // upper teeth
        box(g, cx, cy, u, -4, mouthBottom - 1.2F, 5, mouthBottom, 0xFFFFFFFF); // lower teeth
        box(g, cx, cy, u, -2.5F, mouthBottom - 2.8F, 3.5F, mouthBottom - 1.2F, 0xFF7A1010); // tongue
    }

    private JumpscareHud() {}
}
