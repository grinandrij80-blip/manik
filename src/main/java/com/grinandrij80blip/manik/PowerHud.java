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

        int size = 28;
        int x = minecraft.getWindow().getGuiScaledWidth() - size - 8;
        int y = minecraft.getWindow().getGuiScaledHeight() - size - 8;

        boolean ready = cooldownTicks <= 0;
        boolean active = activeTicks > 0;
        int border = active ? 0xFFFFD83A : ready ? 0xFF36D45A : 0xFF7A7A7A;

        graphics.fill(x - 2, y - 2, x + size + 2, y + size + 2, border);
        graphics.fill(x, y, x + size, y + size, 0xFF1C1C1C);

        // Lightning bolt icon, drawn from rectangles so no texture file is needed.
        int bolt = ready || active ? 0xFFFFD83A : 0xFF555555;
        int bx = x + 8;
        int by = y + 3;
        graphics.fill(bx + 7, by, bx + 12, by + 3, bolt);
        graphics.fill(bx + 5, by + 3, bx + 10, by + 6, bolt);
        graphics.fill(bx + 3, by + 6, bx + 8, by + 9, bolt);
        graphics.fill(bx + 1, by + 9, bx + 12, by + 12, bolt);
        graphics.fill(bx + 5, by + 12, bx + 10, by + 15, bolt);
        graphics.fill(bx + 3, by + 15, bx + 8, by + 18, bolt);
        graphics.fill(bx + 1, by + 18, bx + 6, by + 21, bolt);

        if (!ready) {
            // Dark overlay shrinks from top to bottom as the cooldown runs out.
            int total = PowerManager.COOLDOWN_TICKS;
            int covered = Math.round(size * (cooldownTicks / (float) total));
            graphics.fill(x, y, x + size, y + covered, 0x99000000);
            String seconds = String.valueOf((cooldownTicks + 19) / 20);
            int w = minecraft.font.width(seconds);
            graphics.drawString(minecraft.font, seconds, x + (size - w) / 2, y + (size - 8) / 2, 0xFFFFFFFF);
        } else if (KEY != null) {
            String label = KEY.getTranslatedKeyMessage().getString();
            int w = minecraft.font.width(label);
            graphics.drawString(minecraft.font, label, x + (size - w) / 2, y + size - 9, 0xFF36D45A);
        }
    }

    private PowerHud() {}
}
