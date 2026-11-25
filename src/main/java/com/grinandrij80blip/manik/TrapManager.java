package com.grinandrij80blip.manik;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
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
 * Trap placement: 40 seconds. Crowbar disarm: 7 seconds.
 */
public final class TrapManager {
    public static final int TRAP_COOLDOWN_TICKS = 40 * 20;
    public static final int CROWBAR_COOLDOWN_TICKS = 7 * 20;
    /** Fixed trap damage in health points (8 HP = 4 hearts). */
    public static final float TRAP_DAMAGE = 8.0F;
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

            if (StaminaManager.isDisabled(player)) {
                player.displayClientMessage(Component.literal("Ти оглушений і не можеш ставити капкани."), true);
                return true;
            }

            ItemStack trapStack = new ItemStack(ManikBlocks.TRAP_ITEM.get());
            if (player.getCooldowns().isOnCooldown(trapStack)) {
                player.displayClientMessage(Component.literal("Капкан перезаряджається."), true);
                return true;
            }

            player.getCooldowns().addCooldown(trapStack, TRAP_COOLDOWN_TICKS);
            ClownManager.breakInvisibility(player);
            if (activeLevel != level) {
                clearAll(activeLevel);
                activeLevel = level;
            }
            ACTIVE_TRAPS.add(event.getPos().immutable());
            Sfx.at(level, event.getPos().getX() + 0.5, event.getPos().getY() + 0.5, event.getPos().getZ() + 0.5,
                    net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_CLOSE, 1.0F, 0.7F);
            player.displayClientMessage(Component.literal("Капкан встановлено. Предмет збережено. Перезарядка: 40 секунд."), true);
        }
        return false;
    }

    public static boolean onBlockBreak(BlockEvent.BreakEvent event) {
        if (!event.getState().is(ManikBlocks.TRAP.get())) return false;

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
        Sfx.at(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                net.minecraft.sounds.SoundEvents.SHIELD_BREAK, 1.0F, 0.9F);
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

    /**
     * A trapped survivor frees themselves by using the crowbar (right click anywhere).
     * Same price as disarming: the trap is removed, 7 s crowbar cooldown, one use spent.
     * Returns true when the survivor was trapped and has been freed.
     */
    public static boolean freeSelf(ServerPlayer player, net.minecraft.world.InteractionHand hand, ItemStack crowbar) {
        BlockPos pos = TRAPPED_PLAYERS.get(player.getUUID());
        if (pos == null || activeLevel == null) return false;

        if (player.getCooldowns().isOnCooldown(crowbar)) {
            player.displayClientMessage(Component.literal("Лом перезаряджається."), true);
            return true;
        }

        if (activeLevel.getBlockState(pos).is(ManikBlocks.TRAP.get())) {
            activeLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            Sfx.at(activeLevel, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    net.minecraft.sounds.SoundEvents.SHIELD_BREAK, 1.0F, 0.9F);
        }
        ACTIVE_TRAPS.remove(pos);
        releasePlayersAt(pos);
        player.getCooldowns().addCooldown(crowbar, CROWBAR_COOLDOWN_TICKS);
        CrowbarItem.consumeUse(player, hand, crowbar);
        player.displayClientMessage(Component.literal("Ти визволився з капкана ломом."), true);
        return true;
    }

    private static void releasePlayersAt(BlockPos trapPos) {
        TRAPPED_PLAYERS.entrySet().removeIf(entry -> entry.getValue().equals(trapPos));
    }

    /**
     * Finds any active trap whose block volume touches the survivor's hitbox.
     * Slightly expanding the trap volume also catches a player standing on its
     * top surface, where the two unexpanded boxes only touch at the boundary.
     */
    private static BlockPos findTouchingTrap(ServerPlayer player) {
        if (activeLevel == null) return null;

        AABB playerBox = player.getBoundingBox().inflate(0.02, 0.12, 0.02);
        for (BlockPos pos : ACTIVE_TRAPS) {
            if (!activeLevel.getBlockState(pos).is(ManikBlocks.TRAP.get())) continue;

            AABB trapBox = new AABB(
                    pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0
            ).inflate(0.02, 0.12, 0.02);

            if (playerBox.intersects(trapBox)) return pos;
        }
        return null;
    }

    private static void holdAtTrap(ServerPlayer player, BlockPos pos) {
        double targetX = pos.getX() + 0.5;
        double targetY = pos.getY();
        double targetZ = pos.getZ() + 0.5;

        // Use the ServerPlayer teleport operation when the client has moved,
        // rather than changing only the server-side position with setPos().
        // This sends a position correction to the player's client as well.
        if (player.distanceToSqr(targetX, targetY, targetZ) > 1.0E-4
                || player.getDeltaMovement().lengthSqr() > 1.0E-4) {
            player.teleportTo(activeLevel, targetX, targetY, targetZ,
                    Set.of(), player.getYRot(), player.getXRot(), true);
        }

        // Trapped survivors glow until they get out.
        if (player.tickCount % 10 == 0
                || !player.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING)) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.GLOWING, 40, 0, false, false, false));
        }

        player.setDeltaMovement(0.0, 0.0, 0.0);
        player.setSprinting(false);
        player.setOnGround(true);
    }

    /**
     * Detects contact and repeatedly corrects the captured player's position
     * on the server until the trap is removed by the disarm interaction.
     */
    public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof ServerPlayer player)) return;

        UUID playerId = player.getUUID();

        if (GameConfig.isRunning()
                && activeLevel != null
                && player.level() == activeLevel
                && player.getTags().contains("manik_survivor")
                && !TRAPPED_PLAYERS.containsKey(playerId)) {
            BlockPos touchingTrap = findTouchingTrap(player);
            if (touchingTrap != null) {
                onTrapStepped(activeLevel, touchingTrap, player);
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
                holdAtTrap(player, trappedAt);
            } else {
                TRAPPED_PLAYERS.remove(playerId);
                player.removeEffect(net.minecraft.world.effect.MobEffects.GLOWING);
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

        player.hurt(serverLevel.damageSources().generic(), TRAP_DAMAGE);
        Sfx.at(serverLevel, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                net.minecraft.sounds.SoundEvents.ANVIL_PLACE, 1.0F, 1.4F);
        if (player.isAlive()) {
            BlockPos immutablePos = pos.immutable();
            TRAPPED_PLAYERS.put(player.getUUID(), immutablePos);
            holdAtTrap(player, immutablePos);
            player.displayClientMessage(
                    Component.literal("Ти потрапив у капкан! Втрачено 8 HP (4 серця). Не можеш вибратися, доки капкан не знешкодять."),
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
