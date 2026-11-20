package com.grinandrij80blip.manik;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;

public final class StaminaHud {
    private static int stamina = 100;
    private static int exhaustionTicks;
    private static int jumpBlockedTicks;
    private static boolean active;

    public static void register() {
        AddGuiOverlayLayersEvent.BUS.addListener(event ->
                // Add at the end of VANILLA_ROOT. HOTBAR_MESSAGE belongs to a
                // nested stack, so it cannot be used as a sibling insertion target.
                event.getLayeredDraw().add(
                        Identifier.parse("manik:stamina_hud"),
                        StaminaHud::render
                )
        );
    }

    public static void update(int value, int exhaustion, int jumpBlocked, boolean enabled) {
        stamina = Math.max(0, Math.min(100, value));
        exhaustionTicks = Math.max(0, exhaustion);
        jumpBlockedTicks = Math.max(0, jumpBlocked);
        active = enabled;
        if (!enabled) {
            exhaustionTicks = 0;
            jumpBlockedTicks = 0;
        }
    }

    public static void tickClient() {
        if (jumpBlockedTicks > 0) jumpBlockedTicks--;
    }

    public static boolean isJumpBlocked() {
        return active && jumpBlockedTicks > 0;
    }

    private static void render(GuiGraphics graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!active || minecraft.player == null || minecraft.options.hideGui) return;

        int width = 170;
        int height = 27;
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int x = Math.max(4, (screenWidth - width) / 2);
        int y = minecraft.getWindow().getGuiScaledHeight() - 78;
        int barWidth = width - 8;
        int filled = (int) Math.round(barWidth * stamina / 100.0);

        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF000000);
        graphics.fill(x, y, x + width, y + height, 0xEE171717);
        graphics.drawString(minecraft.font, "СТАМІНА: " + stamina + "%", x + 6, y + 4, 0xFFFFFFFF, false);
        graphics.fill(x + 4, y + 16, x + 4 + barWidth, y + 23, 0xFF3B3B3B);
        if (filled > 0) {
            int color = stamina <= 20 ? 0xFFE04B3F : 0xFF4FC46A;
            graphics.fill(x + 4, y + 16, x + 4 + filled, y + 23, color);
        }

        if (exhaustionTicks > 0) {
            graphics.drawString(minecraft.font,
                    "Спринт: " + ((exhaustionTicks + 19) / 20) + " с",
                    x + width + 8, y + 5, 0xFFFFD36A, false);
        }
        if (jumpBlockedTicks > 0) {
            graphics.drawString(minecraft.font,
                    "Стрибок: " + ((jumpBlockedTicks + 19) / 20) + " с",
                    x, y - 12, 0xFFFF8A80, false);
        }
    }

    private StaminaHud() {}
}
