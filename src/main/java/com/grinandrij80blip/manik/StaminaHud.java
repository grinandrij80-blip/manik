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

        int width = 156;
        int height = 23;
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int x = Math.max(4, (screenWidth - width) / 2);
        int y = minecraft.getWindow().getGuiScaledHeight() - 69;
        int filled = (int) Math.round((width - 6) * stamina / 100.0);

        graphics.fill(x, y, x + width, y + height, 0xDD101010);
        graphics.fill(x, y, x + width, y + 1, 0xFFB8B8B8);
        graphics.drawString(minecraft.font, "СТАМІНА " + stamina + "%", x + 5, y + 3, 0xFFFFFFFF, false);
        graphics.fill(x + 3, y + 14, x + width - 3, y + 20, 0xFF303030);
        if (filled > 0) {
            int color = stamina <= 20 ? 0xFFE04B3F : 0xFF4FC46A;
            graphics.fill(x + 3, y + 14, x + 3 + filled, y + 20, color);
        }

        if (exhaustionTicks > 0) {
            int seconds = (exhaustionTicks + 19) / 20;
            String recovery = "Відновлення: " + seconds + " с";
            graphics.drawString(minecraft.font, recovery, x + width + 6, y + 7, 0xFFFFD36A, false);
        }
    }

    private StaminaHud() {}
}
