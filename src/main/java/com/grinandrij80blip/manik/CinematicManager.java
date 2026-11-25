package com.grinandrij80blip.manik;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;

/**
 * Server timeline of the start and win cinematics. The screen effects are
 * drawn by CinematicHud; the server only sends cue packets and runs the real
 * game logic at the right moment (spawn after the 3-2-1 blackout, lobby after
 * the winner banner).
 */
public final class CinematicManager {
    /** 3-2-1 with a smooth blackout. */
    public static final int START_TICKS = 60;
    /** Win: 0.5 s fade to black, picture back, banner 3.5 s. */
    public static final int END_FADE_TICKS = 10;
    public static final int END_BANNER_TICKS = 70;

    public static final int WINNER_MANIAC = 0;
    public static final int WINNER_SURVIVORS = 1;

    private static Runnable startAction;
    private static int startLeft;
    private static Runnable endAction;
    private static int endLeft;

    public static boolean isStarting() { return startAction != null; }

    public static boolean isEnding() { return endAction != null; }

    public static void beginStart(MinecraftServer server, Runnable action) {
        startAction = action;
        startLeft = START_TICKS;
        broadcast(server, ManikNetwork.CINEMATIC_START, 0);
    }

    public static void beginEnd(MinecraftServer server, int winner, Runnable action) {
        if (endAction != null) return;
        endAction = action;
        endLeft = END_FADE_TICKS + END_BANNER_TICKS;
        broadcast(server, ManikNetwork.CINEMATIC_WIN, winner);
    }

    /** Cancels a pending countdown (for /game stop). Returns true if there was one. */
    public static boolean cancelStart(MinecraftServer server) {
        if (startAction == null) return false;
        startAction = null;
        broadcast(server, ManikNetwork.CINEMATIC_CANCEL, 0);
        return true;
    }

    /** Called when the round is forcibly ended: drop any pending cinematic action. */
    public static void clearEnd(MinecraftServer server) {
        if (endAction != null) {
            endAction = null;
            broadcast(server, ManikNetwork.CINEMATIC_CANCEL, 0);
        }
    }

    /** Tells every client to drop the blackout (a start that could not happen). */
    public static void abortClients(MinecraftServer server) {
        broadcast(server, ManikNetwork.CINEMATIC_CANCEL, 0);
    }

    public static void sendRole(ServerPlayer player, boolean maniac) {
        ManikNetwork.sendCinematic(player, ManikNetwork.CINEMATIC_FADE_IN, maniac ? 0 : 1);
    }

    private static void broadcast(MinecraftServer server, int type, int value) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ManikNetwork.sendCinematic(player, type, value);
        }
    }

    public static void onServerTick(TickEvent.ServerTickEvent.Post event) {
        MinecraftServer server = event.server();
        ManiacSelection.onServerTick(server);
        ClownManager.onServerTick(server);
        if (startAction != null && --startLeft <= 0) {
            Runnable action = startAction;
            startAction = null;
            action.run();
        }
        if (endAction != null && --endLeft <= 0) {
            Runnable action = endAction;
            endAction = null;
            action.run();
        }
    }

    private CinematicManager() {}
}
