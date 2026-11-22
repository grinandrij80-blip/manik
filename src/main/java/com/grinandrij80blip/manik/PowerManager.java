package com.grinandrij80blip.manik;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server side of the maniac super powers.
 * Power 1: Speed III + Jump Boost IV for 6 s (the maniac is nauseous meanwhile) and a
 *          jump scare for every survivor. Cooldown 60 s.
 * Power 2: every survivor glows and is blinded for 6 s. Cooldown 60 s.
 */
public final class PowerManager {
    public static final int DURATION_TICKS = 6 * 20;
    public static final int COOLDOWN_TICKS = 60 * 20;
    public static final int DURATION2_TICKS = 6 * 20;
    public static final int COOLDOWN2_TICKS = 60 * 20;

    private static final Map<UUID, Long> READY_AT = new HashMap<>();
    private static final Map<UUID, Long> READY_AT2 = new HashMap<>();
    private static final Map<UUID, Long> ACTIVE_UNTIL2 = new HashMap<>();
    private static final Map<UUID, Boolean> SYNCED_ENABLED = new HashMap<>();

    private static boolean isManiac(ServerPlayer player) {
        return GameConfig.isRunning() && player.isAlive() && player.getTags().contains("manik_killer");
    }

    private static int left(Map<UUID, Long> map, ServerPlayer player) {
        Long at = map.get(player.getUUID());
        if (at == null) return 0;
        return (int) Math.max(0L, at - player.level().getGameTime());
    }

    public static void activate(ServerPlayer player, int slot) {
        if (!isManiac(player) || StaminaManager.isStunned(player)) return;
        long now = player.level().getGameTime();

        if (slot == 0) {
            if (left(READY_AT, player) > 0) return;
            READY_AT.put(player.getUUID(), now + COOLDOWN_TICKS);
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, DURATION_TICKS, 2, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, DURATION_TICKS, 3, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, DURATION_TICKS, 0, false, false, true));
            jumpscareSurvivors(player);
        } else {
            if (left(READY_AT2, player) > 0) return;
            READY_AT2.put(player.getUUID(), now + COOLDOWN2_TICKS);
            ACTIVE_UNTIL2.put(player.getUUID(), now + DURATION2_TICKS);
            for (ServerPlayer other : player.level().getServer().getPlayerList().getPlayers()) {
                if (other == player || !other.isAlive() || other.isSpectator()
                        || !other.getTags().contains("manik_survivor")) continue;
                other.addEffect(new MobEffectInstance(MobEffects.GLOWING, DURATION2_TICKS, 0, false, false, false));
                other.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, DURATION2_TICKS, 0, false, false, true));
            }
        }
        sync(player);
    }

    /** Screaming maniac face and a roar for every survivor. */
    private static void jumpscareSurvivors(ServerPlayer maniac) {
        for (ServerPlayer other : maniac.level().getServer().getPlayerList().getPlayers()) {
            if (other == maniac || !other.isAlive() || other.isSpectator()
                    || !other.getTags().contains("manik_survivor")) continue;
            ManikNetwork.sendJumpscare(other);
            ManikNetwork.sendShake(other, 25);
            Sfx.toPlayer(other, SoundEvents.GHAST_SCREAM, 1.0F, 0.6F);
            Sfx.toPlayer(other, SoundEvents.ENDERMAN_SCREAM, 1.0F, 0.5F);
        }
    }

    private static void sync(ServerPlayer player) {
        int active1 = 0;
        MobEffectInstance speed = player.getEffect(MobEffects.SPEED);
        if (speed != null && speed.getAmplifier() == 2) active1 = Math.min(speed.getDuration(), DURATION_TICKS);
        int active2 = left(ACTIVE_UNTIL2, player);
        ManikNetwork.sendPower(player, left(READY_AT, player), active1, left(READY_AT2, player), active2, true);
    }

    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof ServerPlayer player)) return;
        UUID id = player.getUUID();
        boolean maniac = isManiac(player);

        if (!maniac) {
            READY_AT.remove(id);
            READY_AT2.remove(id);
            ACTIVE_UNTIL2.remove(id);
            if (Boolean.TRUE.equals(SYNCED_ENABLED.remove(id))) {
                ManikNetwork.sendPower(player, 0, 0, 0, 0, false);
            }
            return;
        }

        // Re-sync once a second (also covers joining, respawning and new rounds).
        if (player.tickCount % 20 == 0 || !Boolean.TRUE.equals(SYNCED_ENABLED.get(id))) {
            SYNCED_ENABLED.put(id, true);
            sync(player);
        }
    }

    /** Maniacs take no fire, lava, drowning or fall damage during a round. Returns true to cancel. */
    public static boolean onLivingHurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isManiac(player)) return false;
        var source = event.getSource();
        boolean immune = source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)
                || source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)
                || source.is(net.minecraft.tags.DamageTypeTags.IS_DROWNING);
        if (immune) player.clearFire();
        return immune;
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        READY_AT.remove(id);
        READY_AT2.remove(id);
        ACTIVE_UNTIL2.remove(id);
        SYNCED_ENABLED.remove(id);
    }

    private PowerManager() {}
}
