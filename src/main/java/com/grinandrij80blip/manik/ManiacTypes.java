package com.grinandrij80blip.manik;

import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Which maniac each player chose for the current round (server side). */
public final class ManiacTypes {
    public static final int BRUTE = 0;
    public static final int CLOWN = 1;

    private static final Map<UUID, Integer> TYPES = new HashMap<>();

    public static int get(Player player) {
        return TYPES.getOrDefault(player.getUUID(), BRUTE);
    }

    public static boolean isClown(Player player) {
        return get(player) == CLOWN;
    }

    public static void set(UUID id, int type) {
        TYPES.put(id, type);
    }

    public static void clear() {
        TYPES.clear();
    }

    private ManiacTypes() {}
}
