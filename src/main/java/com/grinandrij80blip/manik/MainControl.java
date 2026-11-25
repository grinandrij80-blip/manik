package com.grinandrij80blip.manik;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * "Main" player: once one is online, only they (and the server console) can use /game.
 * The role is stored as the player tag manik_main, so it survives relogin.
 */
public final class MainControl {
    public static final String TAG = "manik_main";

    private static boolean anyMainOnline(CommandSourceStack source) {
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            if (player.getTags().contains(TAG)) return true;
        }
        return false;
    }

    /** Gate for the /game command tree. */
    public static boolean canUse(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return true; // console, command blocks
        if (player.getTags().contains(TAG)) return true;
        return !anyMainOnline(source);
    }

    public static int setMain(CommandSourceStack source, ServerPlayer target) {
        boolean isMain = source.getEntity() instanceof ServerPlayer p && p.getTags().contains(TAG);
        boolean console = !(source.getEntity() instanceof ServerPlayer);
        if (!isMain && !console && anyMainOnline(source)) {
            source.sendFailure(Component.literal("Призначити головного може лише поточний головний."));
            return 0;
        }
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            player.removeTag(TAG);
        }
        target.addTag(TAG);
        String name = target.getName().getString();
        source.sendSuccess(() -> Component.literal("MANIK: головний — " + name
                + ". Тепер лише він може користуватися командами /game."), true);
        return 1;
    }

    public static int clearMain(CommandSourceStack source) {
        boolean isMain = source.getEntity() instanceof ServerPlayer p && p.getTags().contains(TAG);
        boolean console = !(source.getEntity() instanceof ServerPlayer);
        if (!isMain && !console && anyMainOnline(source)) {
            source.sendFailure(Component.literal("Зняти головного може лише сам головний."));
            return 0;
        }
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            player.removeTag(TAG);
        }
        source.sendSuccess(() -> Component.literal("MANIK: головного знято, команди знову доступні всім."), true);
        return 1;
    }

    private MainControl() {}
}
