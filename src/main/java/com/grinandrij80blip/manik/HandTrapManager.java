package com.grinandrij80blip.manik;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.level.BlockEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The clown's hand trap: a hand barely poking out of the ground. A survivor who
 * comes within 1.75 blocks is grabbed, lit up and cannot leave the circle of the
 * hand until they win the mini-game (right click on the client, result checked here).
 */
public final class HandTrapManager {
    public static final int COOLDOWN_TICKS = 40 * 20;
    private static final double TRIGGER_RADIUS = 1.75D;
    private static final double HOLD_RADIUS = 3.0D;
    private static final int MIN_ESCAPE_TICKS = 40;

    private static final Set<BlockPos> HANDS = new HashSet<>();
    private static final Map<UUID, BlockPos> GRABBED = new HashMap<>();
    private static final Map<UUID, Integer> SINCE = new HashMap<>();
    private static ServerLevel activeLevel;
    private static int ticks;

    public static boolean onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof ServerPlayer player)
                || !event.getPlacedBlock().is(ManikBlocks.HAND_TRAP.get())) {
            return false;
        }
        if (!GameConfig.isRunning()) return false;

        if (!player.getTags().contains("manik_killer")) {
            player.displayClientMessage(Component.literal("Руку-пастку може ставити лише маніяк."), true);
            return true;
        }
        if (StaminaManager.isDisabled(player)) {
            player.displayClientMessage(Component.literal("Ти не можеш ставити пастки зараз."), true);
            return true;
        }
        ItemStack stack = new ItemStack(ManikBlocks.HAND_TRAP_ITEM.get());
        if (player.getCooldowns().isOnCooldown(stack)) {
            player.displayClientMessage(Component.literal("Рука-пастка перезаряджається."), true);
            return true;
        }
        player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        ClownManager.breakInvisibility(player);
        activeLevel = level;
        HANDS.add(event.getPos().immutable());
        Sfx.at(level, event.getPos().getX() + 0.5, event.getPos().getY() + 0.5, event.getPos().getZ() + 0.5,
                net.minecraft.sounds.SoundEvents.GRAVEL_BREAK, 1.0F, 0.6F);
        player.displayClientMessage(Component.literal("Руку-пастку встановлено. Перезарядка: 40 секунд."), true);
        return false;
    }

    public static boolean onBlockBreak(BlockEvent.BreakEvent event) {
        if (!GameConfig.isRunning() || !event.getState().is(ManikBlocks.HAND_TRAP.get())) return false;
        if (event.getPlayer() instanceof ServerPlayer player) {
            player.displayClientMessage(Component.literal("Руку-пастку не зламати. Жертва має вирватися сама."), true);
        }
        return true;
    }

    public static void beginRound(ServerLevel level) {
        clearAll(activeLevel);
        activeLevel = level;
    }

    public static void clearAll(ServerLevel level) {
        if (level != null) {
            for (BlockPos pos : HANDS) {
                if (level.getBlockState(pos).is(ManikBlocks.HAND_TRAP.get())) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
            MinecraftServer server = level.getServer();
            for (UUID id : new HashSet<>(GRABBED.keySet())) {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p != null) {
                    ManikNetwork.sendGrab(p, false);
                    p.removeEffect(MobEffects.GLOWING);
                }
            }
        }
        HANDS.clear();
        GRABBED.clear();
        SINCE.clear();
        if (activeLevel == level) activeLevel = null;
    }

    private static boolean canBeGrabbed(ServerPlayer p) {
        return p.isAlive() && !p.isSpectator() && p.getTags().contains("manik_survivor")
                && !GRABBED.containsKey(p.getUUID());
    }

    public static void onServerTick(MinecraftServer server) {
        ticks++;
        if (activeLevel == null || !GameConfig.isRunning()) {
            if (!HANDS.isEmpty() || !GRABBED.isEmpty()) clearAll(activeLevel);
            return;
        }

        // Idle hands look for a victim.
        for (BlockPos pos : new HashSet<>(HANDS)) {
            var state = activeLevel.getBlockState(pos);
            if (!state.is(ManikBlocks.HAND_TRAP.get())) {
                HANDS.remove(pos);
                continue;
            }
            if (state.getValue(ManikHandTrapBlock.EXTENDED) || GRABBED.containsValue(pos)) continue;
            double cx = pos.getX() + 0.5, cz = pos.getZ() + 0.5;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (p.level() != activeLevel || !canBeGrabbed(p)) continue;
                double dx = p.getX() - cx, dz = p.getZ() - cz;
                if (dx * dx + dz * dz > TRIGGER_RADIUS * TRIGGER_RADIUS) continue;
                if (Math.abs(p.getY() - pos.getY()) > 1.6) continue;
                grab(p, pos);
                break;
            }
        }

        // Held victims cannot leave the hand's circle.
        Iterator<Map.Entry<UUID, BlockPos>> it = GRABBED.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, BlockPos> entry = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(entry.getKey());
            BlockPos pos = entry.getValue();
            boolean valid = p != null && p.isAlive() && !p.isSpectator()
                    && activeLevel.getBlockState(pos).is(ManikBlocks.HAND_TRAP.get());
            if (!valid) {
                it.remove();
                SINCE.remove(entry.getKey());
                if (p != null) {
                    ManikNetwork.sendGrab(p, false);
                    p.removeEffect(MobEffects.GLOWING);
                }
                continue;
            }
            hold(p, pos);
        }
    }

    private static void grab(ServerPlayer p, BlockPos pos) {
        GRABBED.put(p.getUUID(), pos.immutable());
        SINCE.put(p.getUUID(), ticks);
        activeLevel.setBlock(pos, activeLevel.getBlockState(pos).setValue(ManikHandTrapBlock.EXTENDED, true), 3);
        Sfx.at(activeLevel, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                net.minecraft.sounds.SoundEvents.WITHER_SKELETON_HURT, 1.0F, 0.6F);
        ManikNetwork.sendGrab(p, true);
        ManikNetwork.sendShake(p, 10);
        p.displayClientMessage(Component.literal("Рука схопила тебе! Натисни ПКМ, щоб вирватися."), true);
    }

    private static void hold(ServerPlayer p, BlockPos pos) {
        double cx = pos.getX() + 0.5, cz = pos.getZ() + 0.5;
        double dx = p.getX() - cx, dz = p.getZ() - cz;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > HOLD_RADIUS) {
            double k = (HOLD_RADIUS - 0.1) / dist;
            p.teleportTo(activeLevel, cx + dx * k, p.getY(), cz + dz * k,
                    Set.of(), p.getYRot(), p.getXRot(), true);
        }
        var motion = p.getDeltaMovement();
        p.setDeltaMovement(motion.x * 0.2, Math.min(0.0, motion.y), motion.z * 0.2);
        p.setSprinting(false);
        if (p.tickCount % 10 == 0 || !p.hasEffect(MobEffects.GLOWING)) {
            p.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false, false));
        }
    }

    /** The victim won the mini-game on their client. */
    public static void onEscape(ServerPlayer p) {
        BlockPos pos = GRABBED.get(p.getUUID());
        if (pos == null || activeLevel == null) return;
        Integer since = SINCE.get(p.getUUID());
        if (since == null || ticks - since < MIN_ESCAPE_TICKS) return; // implausibly fast
        GRABBED.remove(p.getUUID());
        SINCE.remove(p.getUUID());
        HANDS.remove(pos);
        if (activeLevel.getBlockState(pos).is(ManikBlocks.HAND_TRAP.get())) {
            activeLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            Sfx.at(activeLevel, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    net.minecraft.sounds.SoundEvents.GRAVEL_BREAK, 1.0F, 1.2F);
        }
        p.removeEffect(MobEffects.GLOWING);
        ManikNetwork.sendGrab(p, false);
        p.displayClientMessage(Component.literal("Ти вирвався з руки!"), true);
    }

    public static boolean isGrabbed(ServerPlayer p) {
        return GRABBED.containsKey(p.getUUID());
    }

    private HandTrapManager() {}
}
