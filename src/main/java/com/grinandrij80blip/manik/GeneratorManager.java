package com.grinandrij80blip.manik;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;

public final class GeneratorManager {
    private static final class State {
        double fuel;
        double power;
    }

    private static final Map<String, State> GENERATORS = new HashMap<>();

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!level.getBlockState(event.getPos()).is(ManikBlocks.GENERATOR.get())) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        State state = state(level, event.getPos());
        ItemStack held = event.getItemStack();

        if (held.is(ManikItems.CANISTER.get()) && state.fuel < 100.0) {
            state.fuel = Math.min(100.0, state.fuel + 50.0);
            if (!player.getAbilities().instabuild) held.shrink(1);
            player.displayClientMessage(Component.literal("Пальне генератора: " + percent(state.fuel) + "%"), true);
            event.setCanceled(true);
            return;
        }

        if (player.isCrouching() && state.fuel >= 100.0 && state.power < 100.0) {
            player.displayClientMessage(Component.literal("Утримуй Shift + ПКМ, дивлячись на генератор, щоб заряджати його."), true);
            event.setCanceled(true);
        }
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer) || !(player.level() instanceof ServerLevel level)) return;

        HitResult hit = player.pick(5.0, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit)) return;
        BlockPos pos = blockHit.getBlockPos();
        if (!level.getBlockState(pos).is(ManikBlocks.GENERATOR.get())) return;

        State state = state(level, pos);
        if (player.isCrouching() && state.fuel >= 100.0 && state.power < 100.0) {
            state.power = Math.min(100.0, state.power + 0.2);
        }

        if (player.tickCount % 5 == 0) {
            String line = state.fuel < 100.0
                    ? "Пальне: " + percent(state.fuel) + "% (1 каністра = 50%)"
                    : "Сила генератора: " + percent(state.power) + "%";
            serverPlayer.displayClientMessage(Component.literal(line), true);
        }
    }

    private static State state(ServerLevel level, BlockPos pos) {
        String key = level.dimension().location() + "|" + pos.asLong();
        return GENERATORS.computeIfAbsent(key, ignored -> new State());
    }

    private static int percent(double value) {
        return (int) Math.floor(value + 0.0001);
    }

    private GeneratorManager() {}
}
