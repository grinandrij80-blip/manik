package com.grinandrij80blip.manik;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * /test maniac &lt;name&gt; and /test stop: lets the main player try any maniac without a real round.
 * The Beekeeper and the Fallen Angel can only be reached through this command.
 */
public final class TestMode {
    private static boolean active;
    private static UUID tester;
    private static final Set<UUID> DUMMIES = new HashSet<>();

    public static boolean isActive() {
        return active;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("test")
                .requires(MainControl::canUse)
                .then(Commands.literal("maniac")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                        List.of("necromancer", "clown", "beekeeper", "angel"), b))
                                .executes(ctx -> start(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("stop").executes(ctx -> stop(ctx.getSource())));
    }

    private static int parse(String raw) {
        String n = raw.trim().toLowerCase();
        return switch (n) {
            case "necromancer", "некромант", "brute" -> ManiacTypes.BRUTE;
            case "clown", "клоун" -> ManiacTypes.CLOWN;
            case "beekeeper", "бджоляр", "пчелочник" -> ManiacTypes.BEEKEEPER;
            case "angel", "ангел", "fallen_angel" -> ManiacTypes.ANGEL;
            default -> -1;
        };
    }

    private static void give(ServerPlayer p, Item item) {
        ItemStack stack = new ItemStack(item);
        if (!p.getInventory().add(stack)) p.drop(stack, false);
    }

    private static int start(CommandSourceStack source, String name) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Команду може виконати лише гравець."));
            return 0;
        }
        int type = parse(name);
        if (type < 0) {
            source.sendFailure(Component.literal("Невідомий маніяк. Доступні: necromancer, clown, beekeeper, angel."));
            return 0;
        }
        if (GameConfig.isRunning() && !active) {
            source.sendFailure(Component.literal("Іде справжня гра — тест неможливий. Спершу /game stop."));
            return 0;
        }
        MinecraftServer server = source.getServer();
        if (active) cleanup(server);

        active = true;
        tester = player.getUUID();
        GameConfig.setRunning(true);
        ManiacTypes.clear();
        ManiacTypes.set(player.getUUID(), type);
        player.addTag("manik_killer");
        player.removeTag("manik_survivor");
        DUMMIES.clear();
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other == player) continue;
            other.removeTag("manik_killer");
            other.addTag("manik_survivor");
            DUMMIES.add(other.getUUID());
        }
        SpectatorManager.beginRound(server);

        give(player, ManikBlocks.TRAP_ITEM.get());
        switch (type) {
            case ManiacTypes.BRUTE -> {
                give(player, ManikItems.MARK_GLOVE.get());
                give(player, ManikItems.TRIPWIRE.get());
            }
            case ManiacTypes.CLOWN -> {
                give(player, ManikItems.CLOWN_HAMMER.get());
                give(player, ManikBlocks.HAND_TRAP_ITEM.get());
            }
            case ManiacTypes.BEEKEEPER -> {
                give(player, ManikItems.SMOKER.get());
                give(player, ManikItems.MINI_HIVE.get());
            }
            default -> {
                give(player, ManikItems.FEATHER_BOW.get());
                give(player, ManikItems.CANDLE_ITEM.get());
            }
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) StaminaManager.beginRound(p);
        PowerManager.syncNow(player);
        source.sendSuccess(() -> Component.literal("ТЕСТ: ти маніяк (" + name.trim() + "). Зупинити: /test stop"
                + (DUMMIES.isEmpty() ? ". Інших гравців немає — цілей для атак не буде." : "")), false);
        return 1;
    }

    private static int stop(CommandSourceStack source) {
        if (!active) {
            source.sendFailure(Component.literal("Тест не запущено."));
            return 0;
        }
        cleanup(source.getServer());
        source.sendSuccess(() -> Component.literal("Тест зупинено."), false);
        return 1;
    }

    /** Light cleanup: only what the test created, not the full round teardown. */
    private static void cleanup(MinecraftServer server) {
        ServerPlayer tp = tester == null ? null : server.getPlayerList().getPlayer(tester);
        ServerLevel level = tp != null && tp.level() instanceof ServerLevel l ? l : server.overworld();

        ExtraManiacs.clearAll(server);
        ClownManager.clear();
        HandTrapManager.clearAll(level);
        TrapManager.clearAll(level);
        TripwireManager.clearAll(server);
        ShockManager.clearAll();
        GloveManager.resetRound();
        SpectatorManager.releaseAll(server);

        List<ServerPlayer> all = new ArrayList<>(server.getPlayerList().getPlayers());
        for (ServerPlayer p : all) {
            p.removeTag("manik_killer");
            p.removeTag("manik_survivor");
            p.removeTag("manik_minion");
        }
        if (tp != null) {
            ManiacBodyManager.removeBody(tp);
            for (int i = 0; i < tp.getInventory().getContainerSize(); i++) {
                ItemStack s = tp.getInventory().getItem(i);
                if (isRoleItem(s)) tp.getInventory().setItem(i, ItemStack.EMPTY);
            }
            tp.removeAllEffects();
        }
        StaminaManager.endRound(all);
        ManiacTypes.clear();
        SpectatorManager.endRound();
        GameConfig.setRunning(false);
        active = false;
        tester = null;
        DUMMIES.clear();
    }

    private static boolean isRoleItem(ItemStack s) {
        return s.is(ManikBlocks.TRAP_ITEM.get()) || s.is(ManikBlocks.HAND_TRAP_ITEM.get())
                || s.is(ManikItems.MARK_GLOVE.get()) || s.is(ManikItems.CLOWN_HAMMER.get())
                || s.is(ManikItems.TRIPWIRE.get()) || s.is(ManikItems.SMOKER.get())
                || s.is(ManikItems.MINI_HIVE.get()) || s.is(ManikItems.FEATHER_BOW.get())
                || s.is(ManikItems.CANDLE_ITEM.get()) || s.is(ManikItems.DAGGER.get())
                || s.is(ManikItems.HONEY_BOTTLE.get());
    }

    /** Ends the test automatically if the tester leaves. */
    public static void onServerTick(MinecraftServer server) {
        if (!active) return;
        if (tester == null || server.getPlayerList().getPlayer(tester) == null) cleanup(server);
    }

    private TestMode() {}
}
