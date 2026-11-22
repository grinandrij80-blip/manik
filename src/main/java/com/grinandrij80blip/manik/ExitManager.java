package com.grinandrij80blip.manik;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Escape phase of a round. A lever starts a 60 s countdown; afterwards a green zone
 * around every exit block opens. Survivors who step into it escape. The round ends
 * when no survivor is left on the map (everyone escaped or died).
 */
public final class ExitManager {
    public static final int COUNTDOWN_TICKS = 60 * 20;
    /** Half of the zone side: the zone is 10 x 10 blocks, centred on the exit block. */
    public static final double ZONE_HALF = 5.0;

    public static final int PHASE_NONE = 0;
    public static final int PHASE_COUNTDOWN = 1;
    public static final int PHASE_OPEN = 2;

    private static int phase = PHASE_NONE;
    private static int ticksLeft;
    private static ServerLevel level;
    private static List<BlockPos> exits = new ArrayList<>();
    private static final Set<UUID> SURVIVORS = new HashSet<>();
    private static final Set<UUID> ESCAPED = new HashSet<>();
    private static final Set<UUID> DEAD = new HashSet<>();

    public static void beginRound(ServerLevel roundLevel, Collection<UUID> survivors) {
        phase = PHASE_NONE;
        ticksLeft = 0;
        level = roundLevel;
        exits = new ArrayList<>();
        SURVIVORS.clear();
        ESCAPED.clear();
        DEAD.clear();
        SURVIVORS.addAll(survivors);
    }

    /** Returns an error message, or null when the countdown has been started. */
    public static String startCountdown(ServerPlayer starter) {
        if (!GameConfig.isRunning() || level == null) return "Гра не запущена.";
        if (phase != PHASE_NONE) return "Таймер виходу вже запущено.";
        exits = SpawnPointManager.get(level, SpawnPointManager.Type.EXIT);
        if (exits.isEmpty()) return "На карті немає блока виходу.";
        phase = PHASE_COUNTDOWN;
        ticksLeft = COUNTDOWN_TICKS;
        MinecraftServer server = starter.level().getServer();
        server.getPlayerList().broadcastSystemMessage(
                Component.literal("MANIK: важіль увімкнено! Вихід відкриється через 60 секунд."), false);
        syncAll(server);
        return null;
    }

    public static void onServerTick(TickEvent.ServerTickEvent.Post event) {
        if (phase == PHASE_NONE) return;
        MinecraftServer server = event.server();
        if (!GameConfig.isRunning() || level == null) {
            reset(server);
            return;
        }

        if (phase == PHASE_COUNTDOWN && --ticksLeft <= 0) {
            phase = PHASE_OPEN;
            ticksLeft = 0;
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("MANIK: вихід відкрито! Біжи в зелену зону!"), false);
            syncAll(server);
        }

        if (server.getTickCount() % 20 == 0) syncAll(server);
        if (phase == PHASE_OPEN && server.getTickCount() % 8 == 0) showZones();

        int alive = 0;
        for (UUID id : new ArrayList<>(SURVIVORS)) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null || ESCAPED.contains(id) || DEAD.contains(id)) continue;
            if (!player.isAlive() || player.isDeadOrDying()) {
                DEAD.add(id);
                continue;
            }
            if (phase == PHASE_OPEN && inAnyZone(player)) {
                ESCAPED.add(id);
                player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
                player.sendSystemMessage(Component.literal("Ти втік! Перемога!"));
                server.getPlayerList().broadcastSystemMessage(
                        Component.literal("MANIK: " + player.getName().getString() + " втік!"), false);
                continue;
            }
            alive++;
        }

        if (alive == 0) {
            StringBuilder names = new StringBuilder();
            for (UUID id : ESCAPED) {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p != null) {
                    if (names.length() > 0) names.append(", ");
                    names.append(p.getName().getString());
                }
            }
            String text = "MANIK: раунд завершено. Втекли: " + ESCAPED.size()
                    + (names.length() > 0 ? " (" + names + ")" : "")
                    + ", загинули: " + DEAD.size() + ".";
            GameCommands.endRound(server, text);
        }
    }

    private static boolean inAnyZone(ServerPlayer player) {
        if (player.level() != level) return false;
        for (BlockPos exit : exits) {
            if (Math.abs(player.getX() - (exit.getX() + 0.5)) <= ZONE_HALF
                    && Math.abs(player.getZ() - (exit.getZ() + 0.5)) <= ZONE_HALF
                    && player.getY() >= exit.getY() - 1.0 && player.getY() < exit.getY() + 2.0) {
                return true;
            }
        }
        return false;
    }

    private static void showZones() {
        DustParticleOptions green = new DustParticleOptions(0x22DD55, 1.6F);
        int cells = (int) (ZONE_HALF * 2);
        for (BlockPos exit : exits) {
            for (int i = 0; i < cells; i++) {
                for (int j = 0; j < cells; j++) {
                    double x = exit.getX() + 0.5 - ZONE_HALF + 0.5 + i;
                    double z = exit.getZ() + 0.5 - ZONE_HALF + 0.5 + j;
                    level.sendParticles(green, x, exit.getY() + 0.1, z, 1, 0.0, 0.0, 0.0, 0.0);
                }
            }
        }
    }

    private static void syncAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ManikNetwork.sendExit(player, phase, ticksLeft);
        }
    }

    /** Clears the escape phase and returns escaped players to normal game mode. */
    public static void reset(MinecraftServer server) {
        for (UUID id : ESCAPED) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) player.setGameMode(server.getDefaultGameType());
        }
        ESCAPED.clear();
        DEAD.clear();
        SURVIVORS.clear();
        exits = new ArrayList<>();
        phase = PHASE_NONE;
        ticksLeft = 0;
        level = null;
        syncAll(server);
    }

    private ExitManager() {}
}
