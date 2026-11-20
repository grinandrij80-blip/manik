package com.grinandrij80blip.manik;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;

public final class StaminaHud {
    private static int stamina = 100;
    private static int exhaustionTicks;
    private static boolean active;

    public static void register() {
        AddGuiOverlayLayersEvent.BUS.addListener(event ->
                event.getLayeredDraw().addAbove(
                        ForgeLayeredDraw.VANILLA_ROOT,
                        Identifier.parse("manik:stamina_hud"),
                        ForgeLayeredDraw.HOTBAR_MESSAGE,
                        StaminaHud::render
                )
        );
    }

    public static void update(int value, int exhaustion, boolean enabled) {
        stamina = Math.max(0, Math.min(100, value));
        exhaustionTicks = Math.max(0, exhaustion);
        active = enabled;
    }

    private static void render(GuiGraphics graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!active || minecraft.player == null || minecraft.options.hideGui) return;

        int x = 12;
        int y = minecraft.getWindow().getGuiScaledHeight() - 58;
        int width = 104;
        int filled = (int) Math.round((width - 4) * stamina / 100.0);

        graphics.fill(x, y, x + width, y + 20, 0x99000000);
        graphics.drawString(minecraft.font, "СТАМІНА " + stamina + "%", x + 3, y + 2, 0xFFFFFFFF, false);
        graphics.fill(x + 2, y + 12, x + width - 2, y + 17, 0xFF303030);
        if (filled > 0) {
            int color = stamina <= 20 ? 0xFFE04B3F : 0xFF4FC46A;
            graphics.fill(x + 2, y + 12, x + 2 + filled, y + 17, color);
        }
        if (exhaustionTicks > 0) {
            int seconds = (exhaustionTicks + 19) / 20;
            graphics.drawString(minecraft.font, "Відновлення: " + seconds + " с",
                    x + width + 6, y + 5, 0xFFFFD36A, false);
        }
    }

    private StaminaHud() {}
}
