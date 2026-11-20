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
import net.minecraftforge.common.util.Result;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class GeneratorManager {
    private static final class GeneratorState {
        double fuel;
        double strength;
        int strengthTicks;
        long lastStrengthGameTime = Long.MIN_VALUE;
    }

    private static final class RefuelSession {
        final String generatorKey;

        RefuelSession(String generatorKey) {
            this.generatorKey = generatorKey;
        }
    }

    private static final Map<String, GeneratorState> GENERATORS = new HashMap<>();
    private static final Map<UUID, RefuelSession> REFUELING = new HashMap<>();
    private static final Set<UUID> USE_INPUT_HELD = new HashSet<>();

    /**
     * Starts the canister interaction on the initial right-click. Actual
     * transfer only happens while the client reports Shift + right-click held.
     */
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!player.isCrouching() || !event.getItemStack().is(ManikItems.CANISTER.get())) return;
        if (!level.getBlockState(event.getPos()).is(ManikBlocks.GENERATOR.get())) return;

        GeneratorState generator = state(level, event.getPos());
        event.setUseBlock(Result.DENY);
        event.setUseItem(Result.DENY);

        if (generator.fuel >= 100.0) {
            player.displayClientMessage(statusMessage(generator), true);
            return;
        }
        if (event.getItemStack().getDamageValue() >= event.getItemStack().getMaxDamage()) {
            REFUELING.remove(player.getUUID());
            player.displayClientMessage(Component.literal("Каністра порожня! Візьми повну каністру."), true);
            return;
        }

        String key = generatorKey(level, event.getPos());
        REFUELING.put(player.getUUID(), new RefuelSession(key));
        player.displayClientMessage(
                Component.literal("Заправка почалася: 4% бензину за секунду. Відпусти Shift або ПКМ, щоб зупинити."),
                true
        );
    }

    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        Player player = event.player();
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        UUID playerId = player.getUUID();
        boolean useInputHeld = USE_INPUT_HELD.contains(playerId);

        HitResult hit = player.pick(5.0, 0.0F, false);
        BlockPos targetedPos = hit instanceof BlockHitResult blockHit ? blockHit.getBlockPos() : null;
        boolean lookingAtGenerator = targetedPos != null
                && level.getBlockState(targetedPos).is(ManikBlocks.GENERATOR.get());

        if (lookingAtGenerator && player.tickCount % 5 == 0) {
            serverPlayer.displayClientMessage(statusMessage(state(level, targetedPos)), true);
        }

        if (!player.isCrouching() || !useInputHeld || !lookingAtGenerator) {
            REFUELING.remove(playerId);
            return;
        }

        GeneratorState generator = state(level, targetedPos);

        // Once the gasoline is full, Shift + held right-click charges strength
        // at exactly one percentage point per 20 server game ticks (one second).
        if (generator.fuel >= 100.0) {
            REFUELING.remove(playerId);
            if (generator.strength < 100.0) {
                long gameTime = level.getGameTime();
                // Prevent two players targeting the same generator from adding
                // multiple strength ticks during the same server tick.
                if (generator.lastStrengthGameTime != gameTime) {
                    generator.lastStrengthGameTime = gameTime;
                    generator.strengthTicks++;
                    if (generator.strengthTicks >= 20) {
                        generator.strength = Math.min(100.0, generator.strength + 1.0);
                        generator.strengthTicks = 0;
                    }
                }
            }
            return;
        }

        RefuelSession session = REFUELING.get(playerId);
        if (session == null) return;

        ItemStack held = player.getMainHandItem();
        if (!session.generatorKey.equals(generatorKey(level, targetedPos))
                || !held.is(ManikItems.CANISTER.get())) {
            REFUELING.remove(playerId);
            return;
        }

        // 4 percentage points per second: transfer 1% every 5 game ticks.
        // The canister durability bar represents its remaining 50% capacity.
        if (player.tickCount % 5 == 0) {
            int used = held.getDamageValue();
            int capacity = held.getMaxDamage();
            if (used >= capacity) {
                REFUELING.remove(playerId);
                serverPlayer.displayClientMessage(Component.literal("Каністра порожня!"), true);
                return;
            }

            double amount = Math.min(1.0, 100.0 - generator.fuel);
            if (amount > 0.0) {
                generator.fuel += amount;
                held.setDamageValue(Math.min(capacity, used + 1));
            }

            if (held.getDamageValue() >= capacity || generator.fuel >= 100.0) {
                REFUELING.remove(playerId);
            }

            serverPlayer.displayClientMessage(
                    Component.literal("Бензин генератора: " + percent(generator.fuel)
                            + "% | Каністра: "
                            + (int) Math.round(100.0 * (capacity - held.getDamageValue()) / capacity) + "%"),
                    true
            );
        }
    }

    public static void setUseInputHeld(ServerPlayer player, boolean held) {
        UUID playerId = player.getUUID();
        if (held) {
            USE_INPUT_HELD.add(playerId);
        } else {
            USE_INPUT_HELD.remove(playerId);
            REFUELING.remove(playerId);
        }
    }

    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            USE_INPUT_HELD.remove(player.getUUID());
            REFUELING.remove(player.getUUID());
        }
    }

    public static void clearAllSessions() {
        USE_INPUT_HELD.clear();
        REFUELING.clear();
    }

    public static void clearGenerator(ServerLevel level, BlockPos pos) {
        String key = generatorKey(level, pos);
        GENERATORS.remove(key);
        REFUELING.entrySet().removeIf(entry -> entry.getValue().generatorKey.equals(key));
    }

    private static Component statusMessage(GeneratorState generator) {
        if (generator.fuel < 100.0) {
            return Component.literal("Бензин генератора: " + percent(generator.fuel) + "%");
        }
        if (generator.strength < 100.0) {
            return Component.literal("Сила генератора: " + percent(generator.strength) + "%");
        }
        return Component.literal("Генератор готовий!");
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

    private GeneratorManager() {
    }
}
