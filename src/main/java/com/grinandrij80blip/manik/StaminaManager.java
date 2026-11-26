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
    // 1.6x scale gives an Enderman-like height while also widening the model
    // and its entity dimensions; each player's own skin/model proportions remain.
    

    private static void setScale(ServerPlayer player, double value) {
        AttributeInstance scale = player.getAttribute(Attributes.SCALE);
        if (scale != null && Math.abs(scale.getBaseValue() - value) > 0.000001D) {
            scale.setBaseValue(value);
            player.refreshDimensions();
        }
    }

    private static void setWalkSpeed(ServerPlayer player, double value) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && Math.abs(speed.getBaseValue() - value) > 0.000001D) {
            speed.setBaseValue(value);
        }
    }
    // 150 points = 100 + 50%. Sprint drains 1/tick (7.5 s), regen 0.5/tick.
    private static final double MAX_STAMINA = 150.0;
    private static final long EXHAUST_TICKS = 140L; // 7 s without sprint after full depletion

    private static final class StaminaState {
        double stamina = MAX_STAMINA;
        long exhaustedUntil;
        long jumpBlockedUntil;
        long stunnedUntil;
        long frozenUntil;
        int lastSentStamina = -1;
        int lastSentExhaustion = -1;
        int lastSentJumpBlocked = -1;
        boolean lastSentVisible;
    }

    private static final Map<UUID, StaminaState> STATES = new HashMap<>();

    /** True while a crowbar hit keeps this maniac stunned. */
    public static boolean isStunned(ServerPlayer player) {
        StaminaState state = STATES.get(player.getUUID());
        return state != null && player.level().getGameTime() < state.stunnedUntil;
    }

    /** True while the maniac is frozen casting power 2 (not a stun: can still be hit). */
    public static boolean isFrozen(ServerPlayer player) {
        StaminaState state = STATES.get(player.getUUID());
        return state != null && player.level().getGameTime() < state.frozenUntil;
    }

    /** The maniac cannot attack, place traps or use items/powers. */
    public static boolean isDisabled(ServerPlayer player) {
        return isStunned(player) || isFrozen(player);
    }

    /** Freezes in place (no move, no jump) without counting as a stun. */
    public static void freezeKiller(ServerPlayer killer, int ticks) {
        StaminaState state = STATES.computeIfAbsent(killer.getUUID(), ignored -> new StaminaState());
        long now = killer.level().getGameTime();
        state.frozenUntil = now + ticks;
        state.jumpBlockedUntil = Math.max(state.jumpBlockedUntil, now + ticks);
        killer.setSprinting(false);
        killer.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 255, false, true, true));
    }

    /**
     * Stuns a maniac for the given number of ticks: no movement (maximum
     * slowness), no jumping (server lock plus the client-synced jump block),
     * and the glove and trap placement check isStunned(). Returns false if
     * the maniac is already stunned, so a second hit cannot stack or extend it.
     */
    public static boolean stunKiller(ServerPlayer killer, int ticks) {
        StaminaState state = STATES.computeIfAbsent(killer.getUUID(), ignored -> new StaminaState());
        long now = killer.level().getGameTime();
        if (now < state.stunnedUntil) return false;

        state.stunnedUntil = now + ticks;
        state.jumpBlockedUntil = Math.max(state.jumpBlockedUntil, now + ticks);
        killer.setSprinting(false);
        killer.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 255, false, true, true));
        return true;
    }

    private static void clearStun(ServerPlayer player, StaminaState state) {
        if (state != null && player.level().getGameTime() < state.stunnedUntil) {
            player.removeEffect(MobEffects.SLOWNESS);
        }
    }

    /** Apply the role lock before vanilla movement processing on the server. */
    public static void enforceNoSprint(TickEvent.PlayerTickEvent.Pre event) {
        if (!(event.player() instanceof ServerPlayer player)) return;
        if (GameConfig.isRunning() && player.getTags().contains("manik_killer")) {
            setScale(player, ManiacTypes.scale(player));
            setWalkSpeed(player, MANIAC_WALK_SPEED);
            player.setSprinting(false);
        }
    }

    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof ServerPlayer player)) return;

        UUID id = player.getUUID();
        boolean participant = GameConfig.isRunning()
                && (player.getTags().contains("manik_killer") || player.getTags().contains("manik_survivor")
                || player.getTags().contains("manik_minion"));

        if (!participant) {
            setScale(player, ExtraManiacs.isMinion(player) ? ExtraManiacs.MINION_SCALE : 1.0D);
            setWalkSpeed(player, SURVIVOR_WALK_SPEED);
            StaminaState removed = STATES.remove(id);
            clearStun(player, removed);
            if (removed != null) ManikNetwork.sendStamina(player, 100, 0, 0, false, true);
            return;
        }

        StaminaState state = STATES.computeIfAbsent(id, ignored -> new StaminaState());
        long now = player.level().getGameTime();
        boolean killer = player.getTags().contains("manik_killer");

        if (killer) {
            setScale(player, ManiacTypes.scale(player));
            setWalkSpeed(player, MANIAC_WALK_SPEED);
            player.setSprinting(false);
            state.stamina = Math.min(MAX_STAMINA, state.stamina + 0.5);
        } else if (now < state.exhaustedUntil) {
            setScale(player, ExtraManiacs.isMinion(player) ? ExtraManiacs.MINION_SCALE : 1.0D);
            setWalkSpeed(player, SURVIVOR_WALK_SPEED);
            player.setSprinting(false);
            state.stamina = Math.min(MAX_STAMINA, state.stamina + 0.5);
        } else if (player.isSprinting()) {
            setScale(player, ExtraManiacs.isMinion(player) ? ExtraManiacs.MINION_SCALE : 1.0D);
            setWalkSpeed(player, SURVIVOR_WALK_SPEED);
            state.stamina = Math.max(0.0, state.stamina - 1.0);
            if (state.stamina <= 0.0) {
                state.exhaustedUntil = now + EXHAUST_TICKS;
                state.jumpBlockedUntil = now + 40L;
                player.setSprinting(false);
                if (!killer) {
                    player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 255, false, true, true));
                }
                player.displayClientMessage(
                        Component.literal("Стаміна вичерпана! Замедлення й заборона стрибка: 2 секунди. Спринт недоступний 7 секунд."),
                        true);
            }
        } else {
            setScale(player, ExtraManiacs.isMinion(player) ? ExtraManiacs.MINION_SCALE : 1.0D);
            setWalkSpeed(player, SURVIVOR_WALK_SPEED);
            state.stamina = Math.min(MAX_STAMINA, state.stamina + 0.5);
        }

        // Survivors get this lock after exhaustion, maniacs only while stunned.
        if (now < state.jumpBlockedUntil) {
            player.setSprinting(false);
            Vec3 movement = player.getDeltaMovement();
            if (movement.y() > 0.0) {
                player.setDeltaMovement(movement.x(), 0.0, movement.z());
                player.setOnGround(true);
            }
        }

        // The Fallen Angel's flight stamina is sent by ExtraManiacs.
        if (killer && ManiacTypes.get(player) == ManiacTypes.ANGEL) return;

        int stamina = (int) Math.round(state.stamina / MAX_STAMINA * 100.0);
        int exhaustion = (int) Math.max(0L, state.exhaustedUntil - now);
        int jumpBlocked = (int) Math.max(0L, state.jumpBlockedUntil - now);

        // Show the bar while sprint is being used and while stamina is
        // recovering. Hide it again once the survivor is back at full stamina.
        boolean visible = !killer && (player.isSprinting() || state.stamina < MAX_STAMINA);

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
        boolean killer = player.getTags().contains("manik_killer");
        setScale(player, killer ? ManiacTypes.scale(player) : 1.0D);
        setWalkSpeed(player, killer ? MANIAC_WALK_SPEED : SURVIVOR_WALK_SPEED);
        STATES.put(player.getUUID(), new StaminaState());
        boolean canSprint = !player.getTags().contains("manik_killer");
        ManikNetwork.sendStamina(player, 100, 0, 0, false, canSprint);
    }

    public static void endRound(List<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            setScale(player, ExtraManiacs.isMinion(player) ? ExtraManiacs.MINION_SCALE : 1.0D);
            setWalkSpeed(player, SURVIVOR_WALK_SPEED);
            clearStun(player, STATES.remove(player.getUUID()));
            ManikNetwork.sendStamina(player, 100, 0, 0, false, true);
        }
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            setScale(player, ExtraManiacs.isMinion(player) ? ExtraManiacs.MINION_SCALE : 1.0D);
            setWalkSpeed(player, SURVIVOR_WALK_SPEED);
            clearStun(player, STATES.remove(player.getUUID()));
        }
    }

    private StaminaManager() {}
}
