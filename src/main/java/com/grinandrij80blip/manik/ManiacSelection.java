package com.grinandrij80blip.manik;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** 10-second maniac choice menu before the round starts; no choice means a random one. */
public final class ManiacSelection {
    public static final int TICKS = 10 * 20;

    private static final Map<UUID, Integer> CHOICES = new LinkedHashMap<>();
    private static boolean active;
    private static int left;
    private static Runnable done;

    public static boolean isActive() { return active; }

    public static void begin(MinecraftServer server, java.util.List<ServerPlayer> maniacs, Runnable onDone) {
        ManiacTypes.clear();
        CHOICES.clear();
        done = onDone;
        left = TICKS;
        active = true;
        for (ServerPlayer player : maniacs) {
            CHOICES.put(player.getUUID(), -1);
            ManikNetwork.sendCinematic(player, ManikNetwork.CINEMATIC_MENU, TICKS);
        }
        if (CHOICES.isEmpty()) finish(server);
    }

    public static void choose(ServerPlayer player, int type) {
        if (!active || !CHOICES.containsKey(player.getUUID())) return;
        if (type != ManiacTypes.BRUTE && type != ManiacTypes.CLOWN) return;
        CHOICES.put(player.getUUID(), type);
    }

    public static boolean cancel(MinecraftServer server) {
        if (!active) return false;
        active = false;
        done = null;
        closeMenus(server);
        return true;
    }

    private static void closeMenus(MinecraftServer server) {
        for (UUID id : CHOICES.keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) ManikNetwork.sendCinematic(player, ManikNetwork.CINEMATIC_MENU_CLOSE, 0);
        }
    }

    private static void finish(MinecraftServer server) {
        active = false;
        for (Map.Entry<UUID, Integer> entry : CHOICES.entrySet()) {
            int type = entry.getValue();
            if (type < 0) type = ThreadLocalRandom.current().nextInt(2);
            ManiacTypes.set(entry.getKey(), type);
        }
        closeMenus(server);
        Runnable action = done;
        done = null;
        if (action != null) action.run();
    }

    public static void onServerTick(MinecraftServer server) {
        if (!active) return;
        boolean all = true;
        for (int type : CHOICES.values()) if (type < 0) all = false;
        if (all || --left <= 0) finish(server);
    }

    private ManiacSelection() {}
}
