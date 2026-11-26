package com.grinandrij80blip.manik;

import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Which maniac each player chose for the current round (server side). */
public final class ManiacTypes {
    public static final int BRUTE = 0;
    public static final int CLOWN = 1;
    public static final int BEEKEEPER = 2;
    public static final int ANGEL = 3;

    private static final Map<UUID, Integer> TYPES = new HashMap<>();

    public static int get(Player player) {
        return TYPES.getOrDefault(player.getUUID(), BRUTE);
    }

    public static boolean isClown(Player player) {
        return get(player) == CLOWN;
    }

    /** Player scale of a maniac: the Fallen Angel is low, the others are tall. */
    public static double scale(Player player) {
        return get(player) == ANGEL ? 0.75D : 1.6D;
    }

    public static void set(UUID id, int type) {
        TYPES.put(id, type);
    }

    public static void clear() {
        TYPES.clear();
    }

    private ManiacTypes() {}
}
