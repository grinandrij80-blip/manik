package com.grinandrij80blip.manik;

/** Client-side copy of "the local player is carrying the flare crate" (synced from the server). */
public final class CarryState {
    private static boolean carrying;

    public static boolean isCarrying() {
        return carrying;
    }

    public static void set(boolean value) {
        carrying = value;
    }

    private CarryState() {}
}
