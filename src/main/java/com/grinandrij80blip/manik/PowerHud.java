package com.grinandrij80blip.manik;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;

/** Client side of the maniac super power: key, and the square in the bottom-right corner. */
public final class PowerHud {
    public static KeyMapping KEY;

    private static boolean enabled;
    private static int cooldownTicks;
    private static int activeTicks;

    public static void register() {
        RegisterKeyMappingsEvent.BUS.addListener(event -> {
            KEY = new KeyMapping("key.manik.power", InputConstants.Type.KEYSYM,
                    InputConstants.KEY_R, KeyMapping.Category.MISC);
            event.register(KEY);
        });
        AddGuiOverlayLayersEvent.BUS.addListener(event ->
                event.getLayeredDraw().add(Identifier.parse("manik:power_hud"), PowerHud::render));
        TickEvent.ClientTickEvent.Post.BUS.addListener(PowerHud::onClientTick);
    }

    public static void update(int cooldown, int active, boolean on) {
        cooldownTicks = Math.max(0, cooldown);
        activeTicks = Math.max(0, active);
        enabled = on;
    }

    private static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        if (cooldownTicks > 0) cooldownTicks--;
        if (activeTicks > 0) activeTicks--;
        Minecraft minecraft = Minecraft.getInstance();
        if (KEY == null) return;
        while (KEY.consumeClick()) {
            if (enabled && minecraft.player != null && minecraft.screen == null && cooldownTicks <= 0) {
                ManikNetwork.sendPowerRequest();
            }
        }
    }

    private static void render(GuiGraphics graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!enabled || minecraft.player == null || minecraft.options.hideGui) return;

        final int radius = 20;       // half-diagonal of the diamond
        final int innerRadius = 15;  // leaves a thick outline
        int cx = minecraft.getWindow().getGuiScaledWidth() - radius - 10;
        int cy = minecraft.getWindow().getGuiScaledHeight() - radius - 10;

        boolean ready = cooldownTicks <= 0;
        boolean active = activeTicks > 0;
        // While recharging, the outline turns from black to white from top to bottom.
        float progress = ready || active ? 1.0F
                : 1.0F - cooldownTicks / (float) PowerManager.COOLDOWN_TICKS;
        int whiteRows = Math.round(progress * (2 * radius + 1));

        for (int dy = -radius; dy <= radius; dy++) {
            int row = dy + radius;
            int outer = radius - Math.abs(dy);
            int inner = innerRadius - Math.abs(dy);
            int color = row < whiteRows ? 0xFFFFFFFF : 0xFF000000;
            int y = cy + dy;
            if (inner < 0) {
                graphics.fill(cx - outer, y, cx + outer + 1, y + 1, color);
            } else {
                graphics.fill(cx - outer, y, cx - inner, y + 1, color);
                graphics.fill(cx + inner + 1, y, cx + outer + 1, y + 1, color);
                graphics.fill(cx - inner, y, cx + inner + 1, y + 1, 0xFF1C1C1C);
            }
        }

        // Angry, screaming maniac face, drawn from rectangles so no texture file is needed.
        boolean lit = ready || active;
        int dark = 0xFF101010;
        int face = lit ? 0xFFE6E6DC : 0xFF6A6A6A;
        int eye = lit ? 0xFFE62020 : 0xFF5A2020;
        int teeth = lit ? 0xFFFFFFFF : 0xFF8A8A8A;
        graphics.fill(cx - 7, cy - 8, cx + 8, cy + 9, dark);            // frame / hood
        graphics.fill(cx - 6, cy - 7, cx + 7, cy + 8, face);            // mask
        graphics.fill(cx - 6, cy - 6, cx - 4, cy - 5, dark);            // left brow (slanted)
        graphics.fill(cx - 4, cy - 5, cx - 2, cy - 4, dark);
        graphics.fill(cx - 2, cy - 4, cx, cy - 3, dark);
        graphics.fill(cx + 5, cy - 6, cx + 7, cy - 5, dark);            // right brow (slanted)
        graphics.fill(cx + 3, cy - 5, cx + 5, cy - 4, dark);
        graphics.fill(cx + 1, cy - 4, cx + 3, cy - 3, dark);
        graphics.fill(cx - 5, cy - 3, cx - 1, cy - 1, eye);             // eyes
        graphics.fill(cx + 2, cy - 3, cx + 6, cy - 1, eye);
        graphics.fill(cx - 1, cy + 1, cx + 2, cy + 3, dark);            // nose
        graphics.fill(cx - 4, cy + 4, cx + 5, cy + 8, dark);            // open mouth
        graphics.fill(cx - 3, cy + 4, cx + 4, cy + 5, teeth);           // upper teeth
        graphics.fill(cx - 2, cy + 7, cx + 3, cy + 8, 0xFF6A0F0F);      // tongue
    }

    private PowerHud() {}
}
