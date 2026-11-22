package com.grinandrij80blip.manik;

import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;

import java.util.Random;

/** Client side: shakes the camera while the local player is electrically stunned. */
public final class ShakeClient {
    private static final Random RANDOM = new Random();
    private static int ticksLeft;
    private static int totalTicks = 1;

    public static void register() {
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> {
            if (ticksLeft > 0) ticksLeft--;
        });
        ViewportEvent.ComputeCameraAngles.BUS.addListener(event -> {
            if (ticksLeft <= 0) return;
            // Strong at first, fading out towards the end.
            float strength = Math.min(1.0F, ticksLeft / (float) Math.max(20, totalTicks / 2));
            event.setRoll(event.getRoll() + (RANDOM.nextFloat() - 0.5F) * 6.0F * strength);
            event.setYaw(event.getYaw() + (RANDOM.nextFloat() - 0.5F) * 3.0F * strength);
            event.setPitch(event.getPitch() + (RANDOM.nextFloat() - 0.5F) * 3.0F * strength);
        });
    }

    public static void start(int ticks) {
        ticksLeft = Math.max(0, ticks);
        totalTicks = Math.max(1, ticks);
    }

    private ShakeClient() {}
}
