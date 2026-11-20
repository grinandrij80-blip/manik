package com.grinandrij80blip.manik;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;

public final class StaminaHud {
    private static int stamina = 100;
    private static int jumpBlockedTicks;
    private static boolean active;
    private static boolean sprintAllowed = true;

    public static void register() {
        AddGuiOverlayLayersEvent.BUS.addListener(event ->
                // Register directly in the root GUI draw stack so the bar is
                // rendered above the ordinary HUD layers.
                event.getLayeredDraw().add(
                        Identifier.parse("manik:stamina_hud"),
                        StaminaHud::render
                )
        );
    }

    public static void update(int value, int exhaustion, int jumpBlocked,
                              boolean enabled, boolean canSprint) {
        stamina = Math.max(0, Math.min(100, value));
        jumpBlockedTicks = Math.max(0, jumpBlocked);
        active = enabled;
        sprintAllowed = canSprint;
    }

    public static boolean isSprintAllowed() {
        return sprintAllowed;
    }

    public static void tickClient() {
        if (jumpBlockedTicks > 0) jumpBlockedTicks--;
    }

    public static boolean isJumpBlocked() {
        // Not tied to the stamina bar: a stunned maniac has no visible bar
        // but must still be unable to jump on the client.
        return jumpBlockedTicks > 0;
    }

    private static void render(GuiGraphics graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!active || minecraft.player == null || minecraft.options.hideGui) return;

        int width = 124;
        int height = 6;
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int x = (screenWidth - width) / 2;
        int y = minecraft.getWindow().getGuiScaledHeight() - 43;
        int filled = (int) Math.round(width * stamina / 100.0);

        graphics.fill(x, y, x + width, y + height, 0xAA262626);
        if (filled > 0) {
            graphics.fill(x, y, x + filled, y + height, 0xFF36D45A);
        }
    }

    private StaminaHud() {}
}
