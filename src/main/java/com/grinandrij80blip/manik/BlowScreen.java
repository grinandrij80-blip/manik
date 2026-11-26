package com.grinandrij80blip.manik;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** "Задуй вогник": click ДУЙ! about 8 times quickly; the flame regrows if you stop. */
public final class BlowScreen extends Screen {
    private static final int NEED = 8;
    private float power;
    private long lastNanos = System.nanoTime();
    private boolean done;

    public BlowScreen() {
        super(Component.literal("Свічка"));
        power = 1.0F;
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new BlowScreen());
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button.builder(Component.literal("ДУЙ!"), b -> blow())
                .bounds(this.width / 2 - 40, this.height / 2 + 50, 80, 22).build());
    }

    private void blow() {
        if (done) return;
        power += 1.0F;
        if (power >= NEED) {
            done = true;
            ManikNetwork.sendBlowDone();
            this.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        long now = System.nanoTime();
        float dt = (now - lastNanos) / 1.0E9F;
        lastNanos = now;
        power = Math.max(1.0F, power - dt * 2.5F); // the flame regrows

        Minecraft mc = Minecraft.getInstance();
        g.fill(0, 0, this.width, this.height, 0xD0080508);
        int cx = this.width / 2, cy = this.height / 2;
        String t = "ЗАДУЙ ВОГНИК!";
        g.drawString(mc.font, t, cx - mc.font.width(t) / 2, cy - 70, 0xFFFFB347);

        float k = 1.0F - (power - 1.0F) / (NEED - 1.0F);
        int h = Math.max(2, Math.round(34 * k));
        int w = Math.max(2, Math.round(14 * k));
        g.fill(cx - 10, cy - 8, cx + 10, cy + 40, 0xFFE9DFC8);          // candle
        g.fill(cx - 1, cy - 14, cx + 1, cy - 8, 0xFF202020);            // wick
        g.fill(cx - w / 2, cy - 14 - h, cx + w / 2, cy - 14, 0xFFFF8A1E); // flame
        g.fill(cx - w / 4, cy - 14 - h / 2, cx + w / 4, cy - 14, 0xFFFFE680);
    }
}
