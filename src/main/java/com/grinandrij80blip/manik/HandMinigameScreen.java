package com.grinandrij80blip.manik;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Random;

/**
 * "Розтисни пальці": a marker sweeps along a bar; press the button while it is in the
 * green zone to pry one finger off. Four fingers must be freed. A miss tightens the grip
 * (the zone shrinks and the last freed finger clamps again).
 */
public final class HandMinigameScreen extends Screen {
    private static final int FINGERS = 4;
    private final Random random = new Random();
    private int freed;
    private float zoneCenter = 0.5F;
    private float zoneHalf = 0.12F;
    private float phase;
    private long lastNanos = System.nanoTime();
    private long flashUntil;
    private boolean flashGood;
    private boolean done;

    public HandMinigameScreen() {
        super(Component.literal("Рука"));
        newZone();
    }

    private void newZone() {
        zoneCenter = 0.2F + random.nextFloat() * 0.6F;
    }

    private float marker() {
        float p = phase % 2.0F;
        return p < 1.0F ? p : 2.0F - p;
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button.builder(Component.literal("РОЗТИСНУТИ!"), b -> press())
                .bounds(this.width / 2 - 60, this.height / 2 + 62, 120, 22).build());
    }

    private void press() {
        if (done) return;
        float m = marker();
        boolean hit = Math.abs(m - zoneCenter) <= zoneHalf;
        flashGood = hit;
        flashUntil = System.currentTimeMillis() + 250;
        if (hit) {
            freed++;
            if (freed >= FINGERS) {
                done = true;
                ManikNetwork.sendHandEscape();
                this.onClose();
                return;
            }
        } else {
            freed = Math.max(0, freed - 1);
            zoneHalf = Math.max(0.07F, zoneHalf - 0.015F);
        }
        newZone();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        long now = System.nanoTime();
        float dt = (now - lastNanos) / 1.0E9F;
        lastNanos = now;
        phase += dt * (0.9F + 0.35F * freed);

        Minecraft mc = Minecraft.getInstance();
        g.fill(0, 0, this.width, this.height, 0xD8100506);
        int cx = this.width / 2, cy = this.height / 2;

        String title = "ВИРВИСЬ З РУКИ!";
        g.drawString(mc.font, title, cx - mc.font.width(title) / 2, cy - 100, 0xFFFF6A5A);
        String sub = "Тисни кнопку, коли маркер у зеленій зоні";
        g.drawString(mc.font, sub, cx - mc.font.width(sub) / 2, cy - 88, 0xFFB8B0B0);

        // the hand: palm and four fingers; freed fingers lift up and turn green
        g.fill(cx - 38, cy - 22, cx + 38, cy + 18, 0xFF8FA088);
        g.fill(cx - 38, cy + 18, cx + 38, cy + 24, 0xFF5A6A54);
        for (int i = 0; i < FINGERS; i++) {
            int fx = cx - 36 + i * 19;
            boolean up = i < freed;
            int top = up ? cy - 62 : cy - 46;
            g.fill(fx, top, fx + 14, cy - 20, up ? 0xFF8CD07A : 0xFFB0B8A0);
            g.fill(fx, top, fx + 14, top + 5, up ? 0xFF3A6A32 : 0xFF3C2E28);
        }
        g.fill(cx - 52, cy - 8, cx - 36, cy + 10, 0xFF8FA088);

        // bar with the green zone and the marker
        int bw = 220, bx = cx - bw / 2, by = cy + 36;
        g.fill(bx - 2, by - 2, bx + bw + 2, by + 16, 0xFF000000);
        g.fill(bx, by, bx + bw, by + 14, 0xFF3A1A1A);
        int zl = bx + Math.round((zoneCenter - zoneHalf) * bw);
        int zr = bx + Math.round((zoneCenter + zoneHalf) * bw);
        g.fill(zl, by, zr, by + 14, 0xFF3CC84A);
        int mx2 = bx + Math.round(marker() * bw);
        g.fill(mx2 - 2, by - 5, mx2 + 2, by + 19, 0xFFFFFFFF);

        if (System.currentTimeMillis() < flashUntil) {
            g.fill(0, 0, this.width, this.height, flashGood ? 0x2200FF00 : 0x33FF0000);
        }
        String progress = freed + " / " + FINGERS;
        g.drawString(mc.font, progress, cx - mc.font.width(progress) / 2, cy + 92, 0xFFFFFFFF);
    }
}
