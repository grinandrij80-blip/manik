package com.grinandrij80blip.manik;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** 10-second choice between the two maniacs, shown as two rectangular cards. */
public final class ManiacSelectScreen extends Screen {
    private static final String[][] INFO = {
            {"МАНІЯК", "Спо1: Ривок — швидкість, стрибок", "Спо2: Око — сліпота виживших", "Зброя: Ніж", "Пастки: Капкан, розтяжка"},
            {"КЛОУН", "Спо1: Невидимість до удару", "Спо2: Коробка з сюрпризом", "Зброя: Циркова кувалда", "Пастки: Капкан, рука"},
    };
    private static final int[] ACCENT = {0xC21B1B, 0xB0388C};

    private final long startNanos = System.nanoTime();
    private final int totalTicks;

    public ManiacSelectScreen(int totalTicks) {
        super(Component.literal("Вибір маніяка"));
        this.totalTicks = Math.max(20, totalTicks);
    }

    private int cardW() { return Math.min(210, (this.width - 50) / 2); }
    private int cardH() { return Math.min(this.height - 70, 230); }
    private int cardX(int i) { return this.width / 2 + (i == 0 ? -cardW() - 6 : 6); }
    private int cardY() { return (this.height - cardH()) / 2 + 8; }

    @Override
    protected void init() {
        for (int i = 0; i < 2; i++) {
            final int type = i;
            this.addRenderableWidget(Button.builder(Component.literal("ОБРАТИ"), b -> choose(type))
                    .bounds(cardX(i) + 8, cardY() + cardH() - 26, cardW() - 16, 20).build());
        }
    }

    private void choose(int type) {
        ManikNetwork.sendManiacChoice(type);
        this.onClose();
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float t = (System.nanoTime() - startNanos) / 50_000_000.0F;
        g.fill(0, 0, this.width, this.height, 0xE6060306);
        Minecraft mc = Minecraft.getInstance();
        String title = "ОБЕРИ МАНІЯКА";
        g.drawString(mc.font, title, (this.width - mc.font.width(title)) / 2, cardY() - 18, 0xFFFFFFFF);

        for (int i = 0; i < 2; i++) {
            int x = cardX(i), y = cardY(), w = cardW(), h = cardH();
            int accent = ACCENT[i];
            g.fill(x, y, x + w, y + h, 0xF0120A0E);
            g.fill(x, y, x + w, y + 3, 0xFF000000 | accent);
            g.fill(x, y + h - 3, x + w, y + h, 0xFF000000 | accent);
            g.fill(x, y, x + 3, y + h, 0xFF000000 | accent);
            g.fill(x + w - 3, y, x + w, y + h, 0xFF000000 | accent);
            g.fill(x + 7, y + 7, x + w - 7, y + 8, 0x66000000 | accent);

            // "rotating" portrait: slow sway and squash like a turntable
            float sway = (float) Math.sin(t * 0.08F + i);
            float u = Math.max(2.0F, Math.min(5.0F, h / 55.0F));
            int cx = x + w / 2 + Math.round(sway * 6.0F);
            int cy = y + 12 + Math.round(11 * u);
            g.fill(x + 12, y + 12, x + w - 12, y + 12 + Math.round(23 * u), 0xFF1A1014);
            float squash = 1.0F - 0.04F * Math.abs(sway);
            if (i == 0) {
                ManiacArt.bruteFace(g, cx, cy, u * squash, 0.55F + 0.45F * (float) Math.abs(Math.sin(t * 0.2F)));
            } else {
                ManiacArt.clownFace(g, cx, cy, u * 0.95F * squash, 1.0F);
            }

            int ty = y + 12 + Math.round(23 * u) + 8;
            g.drawString(mc.font, INFO[i][0], x + (w - mc.font.width(INFO[i][0])) / 2, ty, 0xFF000000 | accent);
            for (int k = 1; k < INFO[i].length; k++) {
                String line = INFO[i][k].replace("Спо1", "Способка 1").replace("Спо2", "Способка 2");
                g.drawString(mc.font, line, x + 10, ty + 8 + k * 11, 0xFFD8D0D0);
            }
        }

        float left = Math.max(0.0F, 1.0F - t / totalTicks);
        int bw = this.width / 2;
        int bx = (this.width - bw) / 2;
        int by = cardY() + cardH() + 10;
        g.fill(bx, by, bx + bw, by + 5, 0xFF2A2A2A);
        g.fill(bx, by, bx + Math.round(bw * left), by + 5, 0xFFE6B93A);
        String hint = "Не обереш — вибір випаде випадково";
        g.drawString(mc.font, hint, (this.width - mc.font.width(hint)) / 2, by + 9, 0xFFAAAAAA);
    }
}
