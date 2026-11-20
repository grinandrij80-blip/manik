package com.grinandrij80blip.manik;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.common.util.Result;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GeneratorManager {
    private static final class GeneratorState {
        double fuel;
    }

    private static final class RefuelSession {
        final String generatorKey;
        double canisterFuelUsed;

        RefuelSession(String generatorKey) {
            this.generatorKey = generatorKey;
        }
    }

    private static final Map<String, GeneratorState> GENERATORS = new HashMap<>();
    private static final Map<UUID, RefuelSession> REFUELING = new HashMap<>();

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!player.isCrouching() || !event.getItemStack().is(ManikItems.CANISTER.get())) return;
        if (!level.getBlockState(event.getPos()).is(ManikBlocks.GENERATOR.get())) return;

        GeneratorState generator = state(level, event.getPos());
        if (generator.fuel >= 100.0) {
            player.displayClientMessage(Component.literal("Бензин генератора: 100%"), true);
            event.setUseBlock(Result.DENY);
            event.setUseItem(Result.DENY);
            return;
        }

        String key = generatorKey(level, event.getPos());
        REFUELING.put(player.getUUID(), new RefuelSession(key));
        player.displayClientMessage(Component.literal("Заправка почалася: +4% бензину за секунду. Відпусти Shift або відведи приціл, щоб зупинити."), true);
        event.setUseBlock(Result.DENY);
            event.setUseItem(Result.DENY);
    }

    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        Player player = event.player();
        if (!(player instanceof ServerPlayer serverPlayer) || !(player.level() instanceof ServerLevel level)) return;

        HitResult hit = player.pick(5.0, 0.0F, false);
        BlockPos targetedPos = hit instanceof BlockHitResult blockHit ? blockHit.getBlockPos() : null;
        boolean lookingAtGenerator = targetedPos != null
                && level.getBlockState(targetedPos).is(ManikBlocks.GENERATOR.get());

        if (lookingAtGenerator && player.tickCount % 5 == 0) {
            GeneratorState targeted = state(level, targetedPos);
            serverPlayer.displayClientMessage(
                    Component.literal("Бензин генератора: " + percent(targeted.fuel) + "%"), true);
        }

        RefuelSession session = REFUELING.get(player.getUUID());
        if (session == null) return;

        ItemStack held = player.getMainHandItem();
        if (!player.isCrouching() || !lookingAtGenerator
                || !session.generatorKey.equals(generatorKey(level, targetedPos))
                || !held.is(ManikItems.CANISTER.get())) {
            REFUELING.remove(player.getUUID());
            return;
        }

        GeneratorState generator = state(level, targetedPos);
        if (generator.fuel >= 100.0) {
            REFUELING.remove(player.getUUID());
            return;
        }

        // 4 percentage points per second = 0.08 percentage points per game tick.
        double amount = Math.min(0.08, Math.min(50.0 - session.canisterFuelUsed, 100.0 - generator.fuel));
        generator.fuel += amount;
        session.canisterFuelUsed += amount;

        if (session.canisterFuelUsed >= 50.0 - 0.0001) {
            if (!player.getAbilities().instabuild) held.shrink(1);
            session.canisterFuelUsed = 0.0;
            if (held.isEmpty() || generator.fuel >= 100.0) {
                REFUELING.remove(player.getUUID());
            }
        }

        if (player.tickCount % 5 == 0) {
            serverPlayer.displayClientMessage(
                    Component.literal("Заправка генератора: " + percent(generator.fuel) + "%"), true);
        }
    }

    private static GeneratorState state(ServerLevel level, BlockPos pos) {
        return GENERATORS.computeIfAbsent(generatorKey(level, pos), ignored -> new GeneratorState());
    }

    private static String generatorKey(ServerLevel level, BlockPos pos) {
        return level.dimension().toString() + "|" + pos.asLong();
    }

    private static int percent(double value) {
        return (int) Math.floor(value + 0.0001);
    }

    private GeneratorManager() {}
}
