package com.grinandrij80blip.manik;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Electric stun effects: lightning sparks around the player and a shaking screen for them. */
public final class ShockManager {
    private static final Map<UUID, Long> UNTIL = new HashMap<>();

    public static void start(ServerPlayer player, int ticks) {
        UNTIL.put(player.getUUID(), player.level().getGameTime() + ticks);
        ManikNetwork.sendShake(player, ticks);
    }

    public static void onServerTick(TickEvent.ServerTickEvent.Post event) {
        if (UNTIL.isEmpty()) return;
        MinecraftServer server = event.server();
        Iterator<Map.Entry<UUID, Long>> it = UNTIL.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || !player.isAlive() || player.level().getGameTime() >= entry.getValue()) {
                it.remove();
                continue;
            }
            if (player.tickCount % 2 == 0 && player.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        player.getX(), player.getY() + player.getBbHeight() * 0.5, player.getZ(),
                        8, 0.45, player.getBbHeight() * 0.45, 0.45, 0.35);
            }
        }
    }

    public static void clearAll() {
        UNTIL.clear();
    }

    private ShockManager() {}
}
