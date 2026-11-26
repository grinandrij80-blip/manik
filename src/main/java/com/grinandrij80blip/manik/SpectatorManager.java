package com.grinandrij80blip.manik;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A player who dies during a round becomes a spectator where they died and cannot
 * switch back until the round ends (the mode is re-enforced every tick).
 */
public final class SpectatorManager {
    private static final Set<UUID> PARTICIPANTS = new HashSet<>();
    private static final Set<UUID> LOCKED = new HashSet<>();
    private static final Map<UUID, Vec3> DEATH_POS = new HashMap<>();

    public static void beginRound(MinecraftServer server) {
        PARTICIPANTS.clear();
        LOCKED.clear();
        DEATH_POS.clear();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PARTICIPANTS.add(player.getUUID());
        }
    }

    public static void endRound() {
        PARTICIPANTS.clear();
        LOCKED.clear();
        DEATH_POS.clear();
    }

    public static void onServerTick(MinecraftServer server) {
        if (!GameConfig.isRunning() || PARTICIPANTS.isEmpty()) return;
        for (UUID id : PARTICIPANTS) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) continue;
            if (player.isDeadOrDying() || !player.isAlive()) {
                if (LOCKED.add(id)) DEATH_POS.put(id, player.position());
                continue;
            }
            if (!LOCKED.contains(id)) continue;
            if (player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
                player.setGameMode(GameType.SPECTATOR);
            }
            Vec3 pos = DEATH_POS.remove(id);
            if (pos != null && player.level() instanceof ServerLevel level) {
                player.teleportTo(level, pos.x, pos.y, pos.z, java.util.Set.of(),
                        player.getYRot(), player.getXRot(), true);
            }
        }
    }

    /** Online dead players who were forced to spectator and can be resurrected. */
    public static java.util.List<ServerPlayer> deadPlayers(MinecraftServer server) {
        java.util.List<ServerPlayer> list = new java.util.ArrayList<>();
        for (UUID id : LOCKED) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null && p.isAlive() && p.getTags().contains("manik_survivor")) list.add(p);
        }
        return list;
    }

    public static void release(ServerPlayer player) {
        LOCKED.remove(player.getUUID());
        DEATH_POS.remove(player.getUUID());
    }

    /** Test stop: everybody locked as a spectator goes back to survival. */
    public static void releaseAll(MinecraftServer server) {
        for (UUID id : new java.util.ArrayList<>(LOCKED)) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null && p.isAlive()) p.setGameMode(GameType.SURVIVAL);
        }
        LOCKED.clear();
        DEATH_POS.clear();
    }

    private SpectatorManager() {}
}
