package com.grinandrij80blip.manik;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server-authoritative MANIK stamina. Sprinting drains 1 point per tick;
 * walking or standing restores 0.5 point per tick. At zero, sprint is
 * disabled for five seconds. Survivors also receive two seconds of strong
 * slowness and cannot jump during that short exhaustion penalty.
 */
public final class StaminaManager {
    private static final class StaminaState {
        double stamina = 100.0;
        long exhaustedUntil;
        long jumpBlockedUntil;
        int lastSentStamina = -1;
        int lastSentExhaustion = -1;
        boolean sentActive;
    }

    private static final Map<UUID, StaminaState> STATES = new HashMap<>();

    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof ServerPlayer player)) return;

        UUID id = player.getUUID();
        boolean participant = GameConfig.isRunning()
                && (player.getTags().contains("manik_killer") || player.getTags().contains("manik_survivor"));

        if (!participant) {
            StaminaState removed = STATES.remove(id);
            if (removed != null) ManikNetwork.sendStamina(player, 100, 0, false);
            return;
        }

        StaminaState state = STATES.computeIfAbsent(id, ignored -> new StaminaState());
        long now = player.level().getGameTime();
        boolean killer = player.getTags().contains("manik_killer");

        if (killer) {
            player.setSprinting(false);
            state.stamina = Math.min(100.0, state.stamina + 0.5);
        } else if (now < state.exhaustedUntil) {
            player.setSprinting(false);
            state.stamina = Math.min(100.0, state.stamina + 0.5);
        } else if (player.isSprinting()) {
            state.stamina = Math.max(0.0, state.stamina - 1.0);
            if (state.stamina <= 0.0) {
                state.exhaustedUntil = now + 100L;
                state.jumpBlockedUntil = now + 40L;
                player.setSprinting(false);
                if (!killer) {
                    player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 255, false, true, true));
                }
                player.displayClientMessage(
                        Component.literal("Стаміна вичерпана! Замедлення й заборона стрибка: 2 секунди. Спринт недоступний 5 секунд."),
                        true);
            }
        } else {
            state.stamina = Math.min(100.0, state.stamina + 0.5);
        }

        // Remove any upward motion during the two-second exhaustion penalty.
        if (!killer && now < state.jumpBlockedUntil) {
            player.setSprinting(false);
            Vec3 movement = player.getDeltaMovement();
            if (movement.y > 0.0) {
                player.setDeltaMovement(movement.x, 0.0, movement.z);
            }
        }

        int stamina = (int) Math.round(state.stamina);
        int exhaustion = (int) Math.max(0L, state.exhaustedUntil - now);
        if (player.tickCount % 5 == 0 || stamina != state.lastSentStamina
                || exhaustion != state.lastSentExhaustion || !state.sentActive) {
            ManikNetwork.sendStamina(player, stamina, exhaustion, true);
            state.lastSentStamina = stamina;
            state.lastSentExhaustion = exhaustion;
            state.sentActive = true;
        }
    }

    public static void beginRound(ServerPlayer player) {
        STATES.put(player.getUUID(), new StaminaState());
        ManikNetwork.sendStamina(player, 100, 0, true);
    }

    public static void endRound(List<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            STATES.remove(player.getUUID());
            ManikNetwork.sendStamina(player, 100, 0, false);
        }
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) STATES.remove(player.getUUID());
    }

    private StaminaManager() {}
}
