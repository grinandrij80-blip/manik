package com.grinandrij80blip.manik;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.event.TickEvent;

/** Client side of the exit countdown: a timer at the top of the screen. */
public final class ExitHud {
    private static int phase;
    private static int ticksLeft;

    public static void register() {
        AddGuiOverlayLayersEvent.BUS.addListener(event ->
                event.getLayeredDraw().add(Identifier.parse("manik:exit_hud"), ExitHud::render));
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> {
            if (ticksLeft > 0) ticksLeft--;
        });
    }

    public static void update(int newPhase, int newTicksLeft) {
        phase = newPhase;
        ticksLeft = Math.max(0, newTicksLeft);
    }

    private static void render(GuiGraphics graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (phase == ExitManager.PHASE_NONE || (phase == ExitManager.PHASE_OPEN && ticksLeft <= 0) || minecraft.player == null || minecraft.options.hideGui) return;

        String text;
        int color;
        if (phase == ExitManager.PHASE_COUNTDOWN) {
            int seconds = (ticksLeft + 19) / 20;
            text = "Вертоліт прилетить через " + (seconds / 60) + ":" + String.format("%02d", seconds % 60);
            color = 0xFFFFFFFF;
        } else {
            text = "ВЕРТОЛІТ ПРИЛЕТІВ!";
            color = 0xFF55FF77;
        }
        int width = minecraft.font.width(text);
        int x = (minecraft.getWindow().getGuiScaledWidth() - width) / 2;
        int y = 14;
        graphics.fill(x - 6, y - 4, x + width + 6, y + 12, 0xAA000000);
        graphics.drawString(minecraft.font, text, x, y, color);
    }

    private ExitHud() {}
}
