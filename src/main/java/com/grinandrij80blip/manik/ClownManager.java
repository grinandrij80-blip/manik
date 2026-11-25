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

    // ---- wheel of fortune (power 2) -------------------------------------------------
    /** Ticks the wheel spins before its result lands (client animation matches this). */
    public static final int WHEEL_TICKS = 60;
    private static final java.util.Map<UUID, int[]> WHEEL = new java.util.HashMap<>();
    private static int serverTicks;

    /** Every living survivor gets their own wheel: 50% poison (like a bad nut), 50% blindness for 10 s. */
    public static void castWheel(ServerPlayer clown) {
        for (ServerPlayer other : clown.level().getServer().getPlayerList().getPlayers()) {
            if (other == clown || !other.isAlive() || other.isSpectator()
                    || !other.getTags().contains("manik_survivor")) continue;
            int result = other.getRandom().nextBoolean() ? 1 : 0; // 1 = poison, 0 = blindness
            WHEEL.put(other.getUUID(), new int[]{serverTicks + WHEEL_TICKS, result});
            ManikNetwork.sendWheel(other, result);
        }
    }

    public static void onServerTick(net.minecraft.server.MinecraftServer server) {
        serverTicks++;
        if (WHEEL.isEmpty()) return;
        var it = WHEEL.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            if (serverTicks < entry.getValue()[0]) continue;
            it.remove();
            ServerPlayer victim = server.getPlayerList().getPlayer(entry.getKey());
            if (victim == null || !victim.isAlive() || victim.isSpectator()
                    || !victim.getTags().contains("manik_survivor") || !GameConfig.isRunning()) continue;
            if (entry.getValue()[1] == 1) {
                victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.POISON, MacadamiaItem.POISON_TICKS, 0, false, true, true));
                victim.displayClientMessage(Component.literal("Колесо вибрало отруту! Вона забере 4 серця."), true);
            } else {
                victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.BLINDNESS, 200, 0, false, true, true));
                victim.displayClientMessage(Component.literal("Колесо вибрало сліпоту на 10 секунд!"), true);
            }
            Sfx.toPlayer(victim, SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 0.5F);
        }
    }

    public static void remove(UUID id) {
        INVISIBLE.remove(id);
    }

    public static void clear() {
        INVISIBLE.clear();
        WHEEL.clear();
    }

    private ClownManager() {}
}
