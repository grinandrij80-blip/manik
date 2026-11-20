package com.grinandrij80blip.manik;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.Result;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Manages reusable traps, survivor capture, cooldowns and crowbar disarming.
 * Trap placement: 55 seconds. Crowbar disarm: 7 seconds.
 */
public final class TrapManager {
    public static final int TRAP_COOLDOWN_TICKS = 55 * 20;
    public static final int CROWBAR_COOLDOWN_TICKS = 7 * 20;
    private static final Set<BlockPos> ACTIVE_TRAPS = new HashSet<>();
    private static final Map<UUID, BlockPos> TRAPPED_PLAYERS = new HashMap<>();
    private static ServerLevel activeLevel;

    public static void beginRound(ServerLevel level) {
        clearAll(activeLevel);
        activeLevel = level;
        ACTIVE_TRAPS.clear();
        TRAPPED_PLAYERS.clear();
    }

    public static boolean onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof ServerPlayer player)
                || !event.getPlacedBlock().is(ManikBlocks.TRAP.get())) {
            return false;
        }

        if (GameConfig.isRunning()) {
            if (!player.getTags().contains("manik_killer")) {
                player.displayClientMessage(Component.literal("Капкани може встановлювати лише маніяк."), true);
                return true;
            }

            ItemStack trapStack = new ItemStack(ManikBlocks.TRAP_ITEM.get());
            if (player.getCooldowns().isOnCooldown(trapStack)) {
                player.displayClientMessage(Component.literal("Капкан перезаряджається."), true);
                return true;
            }

            player.getCooldowns().addCooldown(trapStack, TRAP_COOLDOWN_TICKS);
            if (activeLevel != level) {
                clearAll(activeLevel);
                activeLevel = level;
            }
            ACTIVE_TRAPS.add(event.getPos().immutable());
            player.displayClientMessage(Component.literal("Капкан встановлено. Предмет збережено. Перезарядка: 55 секунд."), true);
        }
        return false;
    }

    /**
     * Trap blocks cannot be mined by hand or ordinary tools. The crowbar's
     * right-click action below is the only intended way to disarm one.
     */
    public static boolean onBlockBreak(BlockEvent.BreakEvent event) {
        if (!event.getState().is(ManikBlocks.TRAP.get())) {
            return false;
        }

        if (event.getPlayer() instanceof ServerPlayer player) {
            player.displayClientMessage(Component.literal("Капкан не можна зламати киркою чи рукою. Використай лом (ПКМ)."), true);
        }
        return true;
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof ServerPlayer player)
                || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND
                || !level.getBlockState(event.getPos()).is(ManikBlocks.TRAP.get())
                || !event.getItemStack().is(ManikItems.CROWBAR.get())) {
            return;
        }

        event.setUseBlock(Result.DENY);
        event.setUseItem(Result.DENY);

        ItemStack crowbar = event.getItemStack();
        if (player.getCooldowns().isOnCooldown(crowbar)) {
            player.displayClientMessage(Component.literal("Лом перезаряджається."), true);
            return;
        }

        BlockPos pos = event.getPos().immutable();
        if (!level.getBlockState(pos).is(ManikBlocks.TRAP.get())) return;

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        ACTIVE_TRAPS.remove(pos);
        releasePlayersAt(pos);
        player.getCooldowns().addCooldown(crowbar, CROWBAR_COOLDOWN_TICKS);

        int nextDamage = crowbar.getDamageValue() + 1;
        if (nextDamage >= crowbar.getMaxDamage()) {
            player.setItemInHand(event.getHand(), ItemStack.EMPTY);
        } else {
            crowbar.setDamageValue(nextDamage);
        }

        player.displayClientMessage(Component.literal("Капкан знешкоджено. Спійманого гравця звільнено. Перезарядка лома: 7 секунд."), true);
    }

    private static void releasePlayersAt(BlockPos trapPos) {
        TRAPPED_PLAYERS.entrySet().removeIf(entry -> entry.getValue().equals(trapPos));
    }

    /**
     * Keeps captured survivors centered on the active trap until another
     * player disarms it. Also displays the killer's trap cooldown countdown.
     */
    public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof ServerPlayer player)) return;

        UUID playerId = player.getUUID();

        // Detect the trap directly below the survivor's feet every server tick.
        // This is reliable even when the trap's low custom collision shape does
        // not cause Block.stepOn to fire on a particular movement edge.
        if (GameConfig.isRunning()
                && activeLevel != null
                && player.level() == activeLevel
                && player.getTags().contains("manik_survivor")
                && player.onGround()
                && !TRAPPED_PLAYERS.containsKey(playerId)) {
            BlockPos underFeet = BlockPos.containing(player.getX(), player.getY() - 0.05, player.getZ());
            if (activeLevel.getBlockState(underFeet).is(ManikBlocks.TRAP.get())) {
                onTrapStepped(activeLevel, underFeet, player);
            }
        }

        BlockPos trappedAt = TRAPPED_PLAYERS.get(playerId);
        if (trappedAt != null) {
            boolean stillCaptured = player.isAlive()
                    && GameConfig.isRunning()
                    && activeLevel != null
                    && player.level() == activeLevel
                    && activeLevel.getBlockState(trappedAt).is(ManikBlocks.TRAP.get());
            if (stillCaptured) {
                player.setPos(trappedAt.getX() + 0.5, trappedAt.getY() + 1.0, trappedAt.getZ() + 0.5);
                player.setDeltaMovement(0.0, 0.0, 0.0);
                player.setSprinting(false);
            } else {
                TRAPPED_PLAYERS.remove(playerId);
            }
        }

        if (!GameConfig.isRunning()
                || !player.getTags().contains("manik_killer")
                || !player.getMainHandItem().is(ManikBlocks.TRAP_ITEM.get())
                || player.tickCount % 20 != 0) {
            return;
        }

        ItemStack trapStack = new ItemStack(ManikBlocks.TRAP_ITEM.get());
        float cooldownProgress = player.getCooldowns().getCooldownPercent(trapStack, 0.0F);
        if (cooldownProgress > 0.0F) {
            int secondsLeft = (int) Math.ceil(cooldownProgress * TRAP_COOLDOWN_TICKS / 20.0F);
            player.displayClientMessage(
                    Component.literal("Перезарядка капкана: " + secondsLeft + " с"), true);
        }
    }

    public static void onTrapStepped(Level level, BlockPos pos, net.minecraft.world.entity.Entity entity) {
        if (!(level instanceof ServerLevel serverLevel)
                || !(entity instanceof ServerPlayer player)
                || !GameConfig.isRunning()
                || activeLevel != serverLevel
                || !player.getTags().contains("manik_survivor")
                || !serverLevel.getBlockState(pos).is(ManikBlocks.TRAP.get())
                || TRAPPED_PLAYERS.containsKey(player.getUUID())) {
            return;
        }

        float damage = player.getMaxHealth() / 3.0F;
        player.hurt(serverLevel.damageSources().generic(), damage);
        if (player.isAlive()) {
            TRAPPED_PLAYERS.put(player.getUUID(), pos.immutable());
            player.setPos(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
            player.setDeltaMovement(0.0, 0.0, 0.0);
            player.displayClientMessage(
                    Component.literal("Ти потрапив у капкан! Втрачено 1/3 здоров'я. Тебе звільнять, коли капкан знешкодять."),
                    true);
        }
    }

    public static void clearAll(ServerLevel level) {
        if (level != null) {
            for (BlockPos pos : ACTIVE_TRAPS) {
                if (level.getBlockState(pos).is(ManikBlocks.TRAP.get())) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        ACTIVE_TRAPS.clear();
        TRAPPED_PLAYERS.clear();
        if (activeLevel == level) activeLevel = null;
    }

    private TrapManager() {}
}
