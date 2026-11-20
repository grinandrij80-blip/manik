package com.grinandrij80blip.manik;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.Result;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Manages traps, reusable trap-item cooldowns and crowbar disarming.
 * Trap placement: 55 seconds. Crowbar disarm: 7 seconds.
 */
public final class TrapManager {
    public static final int TRAP_COOLDOWN_TICKS = 55 * 20;
    public static final int CROWBAR_COOLDOWN_TICKS = 7 * 20;
    private static final Set<BlockPos> ACTIVE_TRAPS = new HashSet<>();
    private static ServerLevel activeLevel;

    public static void beginRound(ServerLevel level) {
        clearAll(activeLevel);
        activeLevel = level;
        ACTIVE_TRAPS.clear();
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

            // Cooldown is attached to the item type. The custom BlockItem restores
            // the stack count after vanilla places the block.
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
        player.getCooldowns().addCooldown(crowbar, CROWBAR_COOLDOWN_TICKS);

        // Spawn a visible item drop in the world (not directly into inventory).
        ItemEntity droppedTrap = new ItemEntity(
                level, pos.getX() + 0.5, pos.getY() + 0.25, pos.getZ() + 0.5,
                new ItemStack(ManikBlocks.TRAP_ITEM.get())
        );
        level.addFreshEntity(droppedTrap);

        int nextDamage = crowbar.getDamageValue() + 1;
        if (nextDamage >= crowbar.getMaxDamage()) {
            player.setItemInHand(event.getHand(), ItemStack.EMPTY);
        } else {
            crowbar.setDamageValue(nextDamage);
        }

        player.displayClientMessage(Component.literal("Капкан знешкоджено й випав на землю. Перезарядка лома: 7 секунд."), true);
    }

    public static void onTrapStepped(Level level, BlockPos pos, net.minecraft.world.entity.Entity entity) {
        if (!(level instanceof ServerLevel serverLevel)
                || !(entity instanceof ServerPlayer player)
                || !GameConfig.isRunning()
                || activeLevel != serverLevel
                || !player.getTags().contains("manik_survivor")
                || !serverLevel.getBlockState(pos).is(ManikBlocks.TRAP.get())) {
            return;
        }

        // A trap briefly slows the survivor; it never deals damage.
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 3, false, true, true));
        serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        ACTIVE_TRAPS.remove(pos.immutable());
        player.displayClientMessage(Component.literal("Ти наступив на капкан! Рух сповільнено на 4 секунди."), true);
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
        if (activeLevel == level) activeLevel = null;
    }

    private TrapManager() {}
}
