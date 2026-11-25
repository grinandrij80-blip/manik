package com.grinandrij80blip.manik;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Clown specifics: invisibility power (ends on the first hammer hit or any trap
 * placement), a squeaky footstep tell while invisible, and the hammer scare.
 */
public final class ClownManager {
    private static final Set<UUID> INVISIBLE = new HashSet<>();

    public static boolean isInvisible(ServerPlayer player) {
        return INVISIBLE.contains(player.getUUID());
    }

    public static void activate(ServerPlayer player) {
        INVISIBLE.add(player.getUUID());
        player.displayClientMessage(Component.literal("Ти невидимий, доки не вдариш або не поставиш пастку."), true);
        Sfx.at(player, SoundEvents.ENDERMAN_TELEPORT, 0.5F, 1.6F);
    }

    /** Called after a hammer hit or a trap placement. */
    public static void breakInvisibility(ServerPlayer player) {
        if (INVISIBLE.remove(player.getUUID())) {
            player.displayClientMessage(Component.literal("Тебе знову видно."), true);
            Sfx.at(player, SoundEvents.ENDERMAN_TELEPORT, 0.5F, 1.0F);
            PowerManager.syncNow(player);
        }
    }

    /** A hammer hit: reveals the clown and flashes a 0.5 s scare on the victim. */
    public static void onHammerHit(ServerPlayer clown, ServerPlayer victim) {
        breakInvisibility(clown);
        ManikNetwork.sendJumpscare(victim, 1);
        ManikNetwork.sendShake(victim, 10);
        Sfx.toPlayer(victim, SoundEvents.ENDERMAN_SCREAM, 0.8F, 1.4F);
    }

    /** A soft rubber-shoe squeak while the clown is invisible and walking. */
    public static void onPlayerTick(ServerPlayer player) {
        if (!INVISIBLE.contains(player.getUUID())) return;
        double dx = player.getX() - player.xo;
        double dz = player.getZ() - player.zo;
        if (dx * dx + dz * dz > 0.0004D && player.tickCount % 9 == 0 && player.onGround()) {
            Sfx.at(player, SoundEvents.SLIME_BLOCK_STEP, 0.55F, 1.5F);
        }
    }

    public static void remove(UUID id) {
        INVISIBLE.remove(id);
    }

    public static void clear() {
        INVISIBLE.clear();
    }

    private ClownManager() {}
}
