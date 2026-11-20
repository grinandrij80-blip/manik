package com.grinandrij80blip.manik;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server-authoritative MANIK stamina. Sprinting drains 1 point per tick;
 * walking or standing restores 0.5 point per tick. At zero, sprint is
 * disabled for five seconds. Survivors also receive two seconds of strong
 * slowness and a server/client jump lock.
 */
public final class StaminaManager {
    private static final double SURVIVOR_WALK_SPEED = 0.1D;
    // 12% faster than a survivor's normal walk, without enabling sprint.
    private static final double MANIAC_WALK_SPEED = 0.112D;

    private static void setWalkSpeed(ServerPlayer player, double value) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && Math.abs(speed.getBaseValue() - value) > 0.000001D) {
            speed.setBaseValue(value);
        }
    }
    private static final class StaminaState {
        double stamina = 100.0;
        long exhaustedUntil;
        long jumpBlockedUntil;
        int lastSentStamina = -1;
        int lastSentExhaustion = -1;
        int lastSentJumpBlocked = -1;
        boolean lastSentVisible;
    }

    private static final Map<UUID, StaminaState> STATES = new HashMap<>();

    /** Apply the role lock before vanilla movement processing on the server. */
    public static void enforceNoSprint(TickEvent.PlayerTickEvent.Pre event) {
        if (!(event.player() instanceof ServerPlayer player)) return;
        if (GameConfig.isRunning() && player.getTags().contains("manik_killer")) {
            setWalkSpeed(player, MANIAC_WALK_SPEED);
            player.setSprinting(false);
        }
    }

    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof ServerPlayer player)) return;

        UUID id = player.getUUID();
        boolean participant = GameConfig.isRunning()
                && (player.getTags().contains("manik_killer") || player.getTags().contains("manik_survivor"));

        if (!participant) {
            setWalkSpeed(player, SURVIVOR_WALK_SPEED);
            StaminaState removed = STATES.remove(id);
            if (removed != null) ManikNetwork.sendStamina(player, 100, 0, 0, false, true);
            return;
        }

        StaminaState state = STATES.computeIfAbsent(id, ignored -> new StaminaState());
        long now = player.level().getGameTime();
        boolean killer = player.getTags().contains("manik_killer");

        if (killer) {
            setWalkSpeed(player, MANIAC_WALK_SPEED);
            player.setSprinting(false);
            state.stamina = Math.min(100.0, state.stamina + 0.5);
        } else if (now < state.exhaustedUntil) {
            setWalkSpeed(player, SURVIVOR_WALK_SPEED);
            player.setSprinting(false);
            state.stamina = Math.min(100.0, state.stamina + 0.5);
        } else if (player.isSprinting()) {
            setWalkSpeed(player, SURVIVOR_WALK_SPEED);
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
            setWalkSpeed(player, SURVIVOR_WALK_SPEED);
            state.stamina = Math.min(100.0, state.stamina + 0.5);
        }

        if (!killer && now < state.jumpBlockedUntil) {
            player.setSprinting(false);
            Vec3 movement = player.getDeltaMovement();
            if (movement.y() > 0.0) {
                player.setDeltaMovement(movement.x(), 0.0, movement.z());
                player.setOnGround(true);
            }
        }

        int stamina = (int) Math.round(state.stamina);
        int exhaustion = (int) Math.max(0L, state.exhaustedUntil - now);
        int jumpBlocked = (int) Math.max(0L, state.jumpBlockedUntil - now);

        // Show the bar while sprint is being used and while stamina is
        // recovering. Hide it again once the survivor is back at full stamina.
        boolean visible = !killer && (player.isSprinting() || state.stamina < 100.0);

        if (player.tickCount % 5 == 0 || stamina != state.lastSentStamina
                || exhaustion != state.lastSentExhaustion
                || jumpBlocked != state.lastSentJumpBlocked
                || visible != state.lastSentVisible) {
            ManikNetwork.sendStamina(player, stamina, exhaustion, jumpBlocked, visible, !killer);
            state.lastSentStamina = stamina;
            state.lastSentExhaustion = exhaustion;
            state.lastSentJumpBlocked = jumpBlocked;
            state.lastSentVisible = visible;
        }
    }

    /**
     * The vanilla jump event itself is not cancellable, but setting the
     * vertical velocity back to zero prevents the jump impulse on the server.
     */
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        StaminaState state = STATES.get(player.getUUID());
        if (state == null || player.level().getGameTime() >= state.jumpBlockedUntil) return;

        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(movement.x(), 0.0, movement.z());
        player.setOnGround(true);
    }

    public static void beginRound(ServerPlayer player) {
        setWalkSpeed(player, player.getTags().contains("manik_killer")
                ? MANIAC_WALK_SPEED : SURVIVOR_WALK_SPEED);
        STATES.put(player.getUUID(), new StaminaState());
        boolean canSprint = !player.getTags().contains("manik_killer");
        ManikNetwork.sendStamina(player, 100, 0, 0, false, canSprint);
    }

    public static void endRound(List<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            setWalkSpeed(player, SURVIVOR_WALK_SPEED);
            STATES.remove(player.getUUID());
            ManikNetwork.sendStamina(player, 100, 0, 0, false, true);
        }
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) STATES.remove(player.getUUID());
    }

    private StaminaManager() {}
}
