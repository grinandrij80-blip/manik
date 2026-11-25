package com.grinandrij80blip.manik;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;

/** Client side of the maniac super powers: keys, and the two diamonds in the bottom-right corner. */
public final class PowerHud {
    public static KeyMapping KEY;
    public static KeyMapping KEY2;

    private static boolean enabled;
    private static int type;
    private static final int[] cooldown = new int[2];
    private static final int[] active = new int[2];

    public static void register() {
        RegisterKeyMappingsEvent.BUS.addListener(event -> {
            KEY = new KeyMapping("key.manik.power", InputConstants.Type.KEYSYM,
                    InputConstants.KEY_R, KeyMapping.Category.MISC);
            KEY2 = new KeyMapping("key.manik.power2", InputConstants.Type.KEYSYM,
                    InputConstants.KEY_G, KeyMapping.Category.MISC);
            event.register(KEY);
            event.register(KEY2);
        });
        AddGuiOverlayLayersEvent.BUS.addListener(event ->
                event.getLayeredDraw().add(Identifier.parse("manik:power_hud"), PowerHud::render));
        TickEvent.ClientTickEvent.Post.BUS.addListener(PowerHud::onClientTick);
    }

    public static void update(int cooldown1, int active1, int cooldown2, int active2, boolean on, int maniacType) {
        type = maniacType;
        cooldown[0] = Math.max(0, cooldown1);
        active[0] = Math.max(0, active1);
        cooldown[1] = Math.max(0, cooldown2);
        active[1] = Math.max(0, active2);
        enabled = on;
    }

    private static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        for (int i = 0; i < 2; i++) {
            if (cooldown[i] > 0) cooldown[i]--;
            if (active[i] > 0) active[i]--;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (KEY == null || KEY2 == null) return;
        boolean canUse = enabled && minecraft.player != null && minecraft.screen == null;
        while (KEY.consumeClick()) {
            if (canUse && cooldown[0] <= 0) ManikNetwork.sendPowerRequest(0);
        }
        while (KEY2.consumeClick()) {
            if (canUse && cooldown[1] <= 0) ManikNetwork.sendPowerRequest(1);
        }
    }

    private static void render(GuiGraphics graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!enabled || minecraft.player == null || minecraft.options.hideGui) return;

        final int radius = 20;
        int cx1 = minecraft.getWindow().getGuiScaledWidth() - radius - 10;
        int cy = minecraft.getWindow().getGuiScaledHeight() - radius - 10;
        int cx0 = cx1 - 2 * radius - 8;

        // Second power (eye) on the left, first power (maniac face) on the right.
        boolean clown = type == 1;
        diamond(graphics, cx0, cy, radius, 1, clown ? PowerManager.CLOWN_COOLDOWN2_TICKS : PowerManager.COOLDOWN2_TICKS);
        if (clown) ManiacArt.giftBox(graphics, cx0, cy + 1, 0.9F, cooldown[1] <= 0 || active[1] > 0);
        else drawEye(graphics, cx0, cy, cooldown[1] <= 0 || active[1] > 0);
        diamond(graphics, cx1, cy, radius, 0, PowerManager.COOLDOWN_TICKS);
        if (clown) ManiacArt.clownFace(graphics, cx1, cy, 0.62F,
                (cooldown[0] <= 0 || active[0] > 0) ? 0.55F : 0.25F);
        else drawFace(graphics, cx1, cy, cooldown[0] <= 0 || active[0] > 0);
    }

    /** Thick diamond outline: white when ready or active, filling from black to white top-down on cooldown. */
    private static void diamond(GuiGraphics graphics, int cx, int cy, int radius, int slot, int totalCooldown) {
        final int innerRadius = radius - 5;
        boolean ready = cooldown[slot] <= 0;
        boolean on = active[slot] > 0;
        float progress = ready || on ? 1.0F : 1.0F - cooldown[slot] / (float) totalCooldown;
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
    }

    private static void drawEye(GuiGraphics g, int cx, int cy, boolean lit) {
        int white = lit ? 0xFFF2F2F2 : 0xFF7A7A7A;
        int iris = lit ? 0xFFE62020 : 0xFF5A2020;
        g.fill(cx - 7, cy - 1, cx + 8, cy + 2, white);
        g.fill(cx - 5, cy - 3, cx + 6, cy - 1, white);
        g.fill(cx - 5, cy + 2, cx + 6, cy + 4, white);
        g.fill(cx - 3, cy - 4, cx + 4, cy - 3, white);
        g.fill(cx - 3, cy + 4, cx + 4, cy + 5, white);
        g.fill(cx - 2, cy - 3, cx + 3, cy + 4, iris);
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFF101010);
    }

    private static void drawFace(GuiGraphics g, int cx, int cy, boolean lit) {
        int dark = 0xFF101010;
        int face = lit ? 0xFFE6E6DC : 0xFF6A6A6A;
        int eye = lit ? 0xFFE62020 : 0xFF5A2020;
        int teeth = lit ? 0xFFFFFFFF : 0xFF8A8A8A;
        g.fill(cx - 7, cy - 8, cx + 8, cy + 9, dark);
        g.fill(cx - 6, cy - 7, cx + 7, cy + 8, face);
        g.fill(cx - 6, cy - 6, cx - 4, cy - 5, dark);
        g.fill(cx - 4, cy - 5, cx - 2, cy - 4, dark);
        g.fill(cx - 2, cy - 4, cx, cy - 3, dark);
        g.fill(cx + 5, cy - 6, cx + 7, cy - 5, dark);
        g.fill(cx + 3, cy - 5, cx + 5, cy - 4, dark);
        g.fill(cx + 1, cy - 4, cx + 3, cy - 3, dark);
        g.fill(cx - 5, cy - 3, cx - 1, cy - 1, eye);
        g.fill(cx + 2, cy - 3, cx + 6, cy - 1, eye);
        g.fill(cx - 1, cy + 1, cx + 2, cy + 3, dark);
        g.fill(cx - 4, cy + 4, cx + 5, cy + 8, dark);
        g.fill(cx - 3, cy + 4, cx + 4, cy + 5, teeth);
        g.fill(cx - 2, cy + 7, cx + 3, cy + 8, 0xFF6A0F0F);
    }

    private PowerHud() {}
}
