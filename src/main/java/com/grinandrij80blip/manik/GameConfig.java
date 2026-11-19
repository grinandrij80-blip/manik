package com.grinandrij80blip.manik;

/**
 * Server-side configuration and session state for MANIK.
 * All mutations are performed by server commands.
 */
public final class GameConfig {
    private static int generators = 3;
    private static int killers = 1;
    private static boolean running = false;

    public static int generators() {
        return generators;
    }

    public static void setGenerators(int value) {
        generators = value;
    }

    public static int killers() {
        return killers;
    }

    public static void setKillers(int value) {
        killers = value;
    }

    public static boolean isRunning() {
        return running;
    }

    public static void setRunning(boolean value) {
        running = value;
    }

    private GameConfig() {
    }
}
