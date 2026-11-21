package com.grinandrij80blip.manik;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server side of the maniac super power: 6 s of Speed III, then a 60 s cooldown. */
public final class PowerManager {
    public static final int DURATION_TICKS = 6 * 20;
    public static final int COOLDOWN_TICKS = 60 * 20;

    private static final Map<UUID, Long> READY_AT = new HashMap<>();
    private static final Map<UUID, Boolean> SYNCED_ENABLED = new HashMap<>();

    private static boolean isManiac(ServerPlayer player) {
        return GameConfig.isRunning() && player.isAlive() && player.getTags().contains("manik_killer");
    }

    private static int cooldownLeft(ServerPlayer player) {
        Long readyAt = READY_AT.get(player.getUUID());
        if (readyAt == null) return 0;
        return (int) Math.max(0L, readyAt - player.level().getGameTime());
    }

    public static void activate(ServerPlayer player) {
        if (!isManiac(player) || StaminaManager.isStunned(player) || cooldownLeft(player) > 0) return;
        READY_AT.put(player.getUUID(), player.level().getGameTime() + COOLDOWN_TICKS);
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, DURATION_TICKS, 2, false, false, true));
        ManikNetwork.sendPower(player, COOLDOWN_TICKS, DURATION_TICKS, true);
    }

    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof ServerPlayer player)) return;
        UUID id = player.getUUID();
        boolean maniac = isManiac(player);

        if (!maniac) {
            READY_AT.remove(id);
            if (Boolean.TRUE.equals(SYNCED_ENABLED.remove(id))) {
                ManikNetwork.sendPower(player, 0, 0, false);
            }
            return;
        }

        // Re-sync once a second (also covers joining, respawning and new rounds).
        if (player.tickCount % 20 == 0 || !Boolean.TRUE.equals(SYNCED_ENABLED.get(id))) {
            SYNCED_ENABLED.put(id, true);
            MobEffectInstance speed = player.getEffect(MobEffects.SPEED);
            int active = speed != null && speed.getAmplifier() == 2 ? Math.min(speed.getDuration(), DURATION_TICKS) : 0;
            ManikNetwork.sendPower(player, cooldownLeft(player), active, true);
        }
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        READY_AT.remove(id);
        SYNCED_ENABLED.remove(id);
    }

    private PowerManager() {}
}
