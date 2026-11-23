package com.grinandrij80blip.manik;

/** Client-side copy of "the local player is carrying the flare crate" (synced from the server). */
public final class CarryState {
    private static boolean carrying;
    private static int carrierId = -1;

    public static boolean isCarrying() {
        return carrying;
    }

    public static void set(boolean value) {
        carrying = value;
    }

    /** Entity id of the player who carries the crate right now (for everyone to see), or -1. */
    public static int carrierId() {
        return carrierId;
    }

    public static void setCarrierId(int id) {
        carrierId = id;
    }

    private CarryState() {}
}
