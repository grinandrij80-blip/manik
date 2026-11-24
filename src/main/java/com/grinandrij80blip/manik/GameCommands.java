package com.grinandrij80blip.manik;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraftforge.event.RegisterCommandsEvent;

public final class GameCommands {
    private static ServerLevel activeLevel;
    private static final List<BlockPos> ACTIVE_GENERATORS = new ArrayList<>();
    private static final List<UUID> ACTIVE_ITEMS = new ArrayList<>();

    private static boolean readyAnnounced;

    /** The exit opens once the required number of generators is ready; the spare one is not needed. */
    public static boolean areAllGeneratorsReady(ServerLevel level) {
        if (!GameConfig.isRunning() || activeLevel != level || ACTIVE_GENERATORS.isEmpty()) return false;
        int ready = 0;
        for (BlockPos pos : ACTIVE_GENERATORS) {
            if (level.getBlockState(pos).is(ManikBlocks.GENERATOR.get())
                    && GeneratorManager.isGeneratorReady(level, pos)) ready++;
        }
        return ready >= GameConfig.generators();
    }

    /** True only the first time it is called in a round (to announce the exit opening once). */
    public static boolean markReadyAnnounced() {
        if (readyAnnounced) return false;
        readyAnnounced = true;
        return true;
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("game")
                .then(Commands.literal("start")
                        .executes(context -> start(context.getSource(), List.of()))
                        .then(Commands.argument("maniacs", net.minecraft.commands.arguments.EntityArgument.players())
                                .executes(context -> start(context.getSource(),
                                        new ArrayList<>(net.minecraft.commands.arguments.EntityArgument
                                                .getPlayers(context, "maniacs"))))))
                .then(Commands.literal("stop").executes(context -> stop(context.getSource())))
                .then(Commands.literal("status").executes(context -> status(context.getSource())))
                .then(Commands.literal("killer")
                        .then(Commands.argument("number", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 100))
                                .executes(context -> setKillers(context.getSource(),
                                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "number")))))
                .then(Commands.literal("menu")
                        .then(Commands.literal("generators")
                                .then(Commands.argument("number", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 100))
                                        .executes(context -> setGenerators(context.getSource(),
                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "number")))))));
    }

    private static int setKillers(CommandSourceStack source, int number) {
        GameConfig.load(source.getLevel());
        if (GameConfig.isRunning()) {
            source.sendFailure(Component.literal("Зупини гру через /game stop перед зміною налаштувань."));
            return 0;
        }
        GameConfig.setKillers(number);
        source.sendSuccess(() -> Component.literal("Кількість маніяків: " + number), true);
        return 1;
    }

    private static int setGenerators(CommandSourceStack source, int number) {
        GameConfig.load(source.getLevel());
        if (GameConfig.isRunning()) {
            source.sendFailure(Component.literal("Зупини гру через /game stop перед зміною налаштувань."));
            return 0;
        }
        GameConfig.setGenerators(number);
        source.sendSuccess(() -> Component.literal("Кількість генераторів: " + number), true);
        return 1;
    }

    private static int start(CommandSourceStack source, List<ServerPlayer> chosenManiacs) {
        GameConfig.load(source.getLevel());
        if (GameConfig.isRunning()) {
            source.sendFailure(Component.literal("Гра MANIK уже запущена."));
            return 0;
        }
        int players = source.getServer().getPlayerList().getPlayers().size();
        // Players named in /game start <nick> are always maniacs; any missing ones are drawn at random.
        int killers = Math.max(GameConfig.killers(), chosenManiacs.size());
        if (players < 2) {
            source.sendFailure(Component.literal("Потрібно щонайменше 2 гравці."));
            return 0;
        }
        if (killers >= players) {
            source.sendFailure(Component.literal("Маніяків має бути менше, ніж гравців, щоб залишився хоча б один виживший."));
            return 0;
        }

        ServerLevel level = source.getLevel();
        List<BlockPos> killerSpawns = SpawnPointManager.get(level, SpawnPointManager.Type.KILLER);
        List<BlockPos> survivorSpawns = SpawnPointManager.get(level, SpawnPointManager.Type.SURVIVOR);
        int canisterCount = GameConfig.generators() * 2 + 3;
        int availableItemSpawns = SpawnPointManager.get(level, SpawnPointManager.Type.ITEM).size();
        if (availableItemSpawns < canisterCount) {
            source.sendFailure(Component.literal("Недостатньо жовтих точок появи предметів для каністр: потрібно "
                    + canisterCount + ", є " + availableItemSpawns + ". Додай ще "
                    + (canisterCount - availableItemSpawns) + " точок."));
            return 0;
        }
        int availableGenerators = SpawnPointManager.get(level, SpawnPointManager.Type.GENERATOR).size();
        // One spare generator is always placed, so nobody can camp the last one.
        int generatorCount = GameConfig.generators() + 1;
        if (availableGenerators < generatorCount) {
            source.sendFailure(Component.literal("Недостатньо синіх точок появи генераторів: потрібно ще "
                    + (generatorCount - availableGenerators) + " (потрібно на 1 більше, ніж у налаштуванні: один запасний)."));
            return 0;
        }
        if (killerSpawns.size() < killers) {
            source.sendFailure(Component.literal("Недостатньо червоних точок спавну: потрібно ще "
                    + (killers - killerSpawns.size()) + "."));
            return 0;
        }
        if (survivorSpawns.size() < players - killers) {
            source.sendFailure(Component.literal("Недостатньо зелених точок спавну: потрібно ще "
                    + (players - killers - survivorSpawns.size()) + "."));
            return 0;
        }

        if (CinematicManager.isStarting() || CinematicManager.isEnding()) {
            source.sendFailure(Component.literal("Зараз іде відлік або кінцівка раунду — зачекай."));
            return 0;
        }
        MinecraftServer server = source.getServer();
        final int finalKillers = killers;
        CinematicManager.beginStart(server, () -> runStart(server, level, chosenManiacs, finalKillers));
        return 1;
    }

    /** Runs after the 3-2-1 blackout: spawns everything and teleports the players. */
    private static void runStart(MinecraftServer server, ServerLevel level,
                                 List<ServerPlayer> chosenManiacs, int killers) {
        if (GameConfig.isRunning()) return;
        chosenManiacs = new ArrayList<>(chosenManiacs);
        chosenManiacs.removeIf(p -> !server.getPlayerList().getPlayers().contains(p));
        int players = server.getPlayerList().getPlayers().size();
        killers = Math.max(killers, chosenManiacs.size());
        if (players < 2 || killers >= players) {
            CinematicManager.abortClients(server);
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("MANIK: старт скасовано — гравців стало замало."), false);
            return;
        }
        List<BlockPos> killerSpawns = SpawnPointManager.get(level, SpawnPointManager.Type.KILLER);
        List<BlockPos> survivorSpawns = SpawnPointManager.get(level, SpawnPointManager.Type.SURVIVOR);
        int canisterCount = GameConfig.generators() * 2 + 3;
        int generatorCount = GameConfig.generators() + 1;
        List<ServerPlayer> shuffledPlayers = new ArrayList<>(server.getPlayerList().getPlayers());
        Collections.shuffle(shuffledPlayers);
        shuffledPlayers.removeAll(chosenManiacs);
        shuffledPlayers.addAll(0, chosenManiacs);
        Collections.shuffle(killerSpawns);
        Collections.shuffle(survivorSpawns);

        ACTIVE_GENERATORS.clear();
        ACTIVE_ITEMS.clear();
        GloveManager.resetRound();
        TrapManager.beginRound(level);
        TripwireManager.clearAll(server);
        List<BlockPos> generatorSpawns = SpawnPointManager.get(level, SpawnPointManager.Type.GENERATOR);
        Collections.shuffle(generatorSpawns);
        readyAnnounced = false;
        for (int i = 0; i < generatorCount; i++) {
            BlockPos generatorPos = generatorSpawns.get(i);
            level.setBlockAndUpdate(generatorPos, ManikBlocks.GENERATOR.get().defaultBlockState());
            ACTIVE_GENERATORS.add(generatorPos.immutable());
        }
        activeLevel = level;

        List<BlockPos> itemSpawns = SpawnPointManager.get(level, SpawnPointManager.Type.ITEM);
        Collections.shuffle(itemSpawns);
        for (int i = 0; i < canisterCount; i++) {
            BlockPos itemPos = itemSpawns.get(i);
            net.minecraft.world.entity.item.ItemEntity canister = new net.minecraft.world.entity.item.ItemEntity(
                    level, itemPos.getX() + 0.5, itemPos.getY() + 0.05, itemPos.getZ() + 0.5,
                    new net.minecraft.world.item.ItemStack(ManikItems.CANISTER.get()));
            canister.setUnlimitedLifetime();
            if (level.addFreshEntity(canister)) ACTIVE_ITEMS.add(canister.getUUID());
        }

        // Remaining item points receive only utility loot: 50% crowbar, 50% medkit. Traps are issued to killers.
        for (int i = canisterCount; i < itemSpawns.size(); i++) {
            BlockPos itemPos = itemSpawns.get(i);
            // 15% stun gun, 10% nut, 30% crowbar, 25% medkit, 20% wrench.
            int utilityRoll = java.util.concurrent.ThreadLocalRandom.current().nextInt(100);
            net.minecraft.world.item.Item utilityChoice = utilityRoll < 15 ? ManikItems.STUN_GUN.get()
                    : utilityRoll < 25 ? ManikItems.MACADAMIA.get()
                    : utilityRoll < 55 ? ManikItems.CROWBAR.get()
                    : utilityRoll < 80 ? ManikItems.MEDKIT.get() : ManikItems.WRENCH.get();
            net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(utilityChoice);
            net.minecraft.world.entity.item.ItemEntity utilityItem = new net.minecraft.world.entity.item.ItemEntity(
                    level, itemPos.getX() + 0.5, itemPos.getY() + 0.05, itemPos.getZ() + 0.5, stack);
            utilityItem.setUnlimitedLifetime();
            if (level.addFreshEntity(utilityItem)) ACTIVE_ITEMS.add(utilityItem.getUUID());
        }

        List<UUID> survivorIds = new ArrayList<>();
        for (int i = 0; i < shuffledPlayers.size(); i++) {
            ServerPlayer player = shuffledPlayers.get(i);
            boolean killer = i < killers;
            if (!killer) survivorIds.add(player.getUUID());
            BlockPos spawn = killer ? killerSpawns.get(i) : survivorSpawns.get(i - killers);
            player.removeTag("manik_killer");
            player.removeTag("manik_survivor");
            player.addTag(killer ? "manik_killer" : "manik_survivor");

            // A trap is a role item, not map loot: remove any leftover copy,
            // then give exactly one directly to each killer.
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                if (player.getInventory().getItem(slot).is(ManikBlocks.TRAP_ITEM.get())
                        || player.getInventory().getItem(slot).is(ManikItems.MARK_GLOVE.get())
                        || player.getInventory().getItem(slot).is(ManikItems.TRIPWIRE.get())) {
                    player.getInventory().setItem(slot, net.minecraft.world.item.ItemStack.EMPTY);
                }
            }
            if (killer) {
                int emptySlot = -1;
                for (int slot = 0; slot < 36; slot++) {
                    if (player.getInventory().getItem(slot).isEmpty()) {
                        emptySlot = slot;
                        break;
                    }
                }
                if (emptySlot >= 0) {
                    player.getInventory().setItem(emptySlot,
                            new net.minecraft.world.item.ItemStack(ManikBlocks.TRAP_ITEM.get()));
                    player.sendSystemMessage(Component.literal("Тобі видано капкан. Він багаторазовий і має перезарядку."));
                } else {
                    player.sendSystemMessage(Component.literal("У інвентарі немає вільного місця для капкана! Звільни слот."));
                }

                int gloveSlot = -1;
                for (int slot = 0; slot < 36; slot++) {
                    if (player.getInventory().getItem(slot).isEmpty()) {
                        gloveSlot = slot;
                        break;
                    }
                }
                if (gloveSlot >= 0) {
                    player.getInventory().setItem(gloveSlot,
                            new net.minecraft.world.item.ItemStack(ManikItems.MARK_GLOVE.get()));
                    player.sendSystemMessage(Component.literal(
                            "Тобі видано рукавицю: влучання знімає 8 HP (4 серця). Перезарядка: 12 секунд."));
                } else {
                    player.sendSystemMessage(Component.literal("Немає вільного місця для рукавиці! Звільни слот."));
                }

                int wireSlot = -1;
                for (int slot = 0; slot < 36; slot++) {
                    if (player.getInventory().getItem(slot).isEmpty()) {
                        wireSlot = slot;
                        break;
                    }
                }
                if (wireSlot >= 0) {
                    player.getInventory().setItem(wireSlot,
                            new net.minecraft.world.item.ItemStack(ManikItems.TRIPWIRE.get()));
                    player.sendSystemMessage(Component.literal(
                            "Тобі видано розтяжку: ПКМ по одній поверхні, потім по іншій (до 5 блоків). Станить на 5 секунд."));
                } else {
                    player.sendSystemMessage(Component.literal("Немає вільного місця для розтяжки! Звільни слот."));
                }
            }
            player.teleportTo(level, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                    Set.of(), player.getYRot(), player.getXRot(), true);
            player.sendSystemMessage(Component.literal(killer ? "Твоя роль: МАНІЯК" : "Твоя роль: ВИЖИВШИЙ"));
            CinematicManager.sendRole(player, killer);
            StaminaManager.beginRound(player);
        }

        ExitManager.beginRound(level, survivorIds);
        CarryManager.beginRound(level);
        GameConfig.setRunning(true);
        server.getPlayerList().broadcastSystemMessage(
                Component.literal("MANIK: гру розпочато! Маніяків: " + killers
                        + ", виживших: " + (players - killers) + "."), false);
    }

    private static int stop(CommandSourceStack source) {
        if (CinematicManager.cancelStart(source.getServer())) {
            source.sendSuccess(() -> Component.literal("MANIK: відлік старту скасовано."), true);
            return 1;
        }
        if (!GameConfig.isRunning()) {
            source.sendFailure(Component.literal("Гра MANIK зараз не запущена."));
            return 0;
        }
        endRound(source.getServer(),
                "MANIK: гру зупинено. Генератори й ігрові предмети прибрано, точки спавну збережено, ролі скинуто.");
        return 1;
    }

    /** Round finished by the rules: win cinematic first, then the normal cleanup and lobby. */
    public static void finishRound(MinecraftServer server, String message, int winner) {
        if (CinematicManager.isEnding()) return;
        CinematicManager.beginEnd(server, winner, () -> endRound(server, message));
    }

    /** Ends the running round (command or escape finish): cleans the map and resets roles. */
    public static void endRound(net.minecraft.server.MinecraftServer server, String message) {
        GameConfig.setRunning(false);
        CinematicManager.clearEnd(server);
        GloveManager.resetRound();
        ExitManager.reset(server);
        TripwireManager.clearAll(server);
        ShockManager.clearAll();
        ServerLevel cleanupLevel = activeLevel;
        TrapManager.clearAll(cleanupLevel);
        if (cleanupLevel != null) {
            for (UUID itemId : ACTIVE_ITEMS) {
                net.minecraft.world.entity.Entity entity = cleanupLevel.getEntity(itemId);
                if (entity != null) entity.discard();
            }
            for (BlockPos pos : ACTIVE_GENERATORS) {
                if (cleanupLevel.getBlockState(pos).is(ManikBlocks.GENERATOR.get())) {
                    cleanupLevel.setBlockAndUpdate(pos, ManikBlocks.GENERATOR_SPAWN.get().defaultBlockState());
                }
                GeneratorManager.clearGenerator(cleanupLevel, pos);
            }
        }
        ACTIVE_ITEMS.clear();
        ACTIVE_GENERATORS.clear();
        activeLevel = null;
        GeneratorManager.clearAllSessions();
        StaminaManager.endRound(server.getPlayerList().getPlayers());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.removeTag("manik_killer");
            player.removeTag("manik_survivor");
        }
        RoundCleanup.finish(server, cleanupLevel != null ? cleanupLevel : server.overworld());
        server.getPlayerList().broadcastSystemMessage(Component.literal(message), false);
    }

    private static int status(CommandSourceStack source) {
        GameConfig.load(source.getLevel());
        String state = GameConfig.isRunning() ? "запущена" : "зупинена";
        source.sendSuccess(() -> Component.literal("MANIK: " + state
                + " | маніяків: " + GameConfig.killers()
                + " | генераторів для виходу: " + GameConfig.generators() + " (+1 запасний)"), false);
        return 1;
    }

    private GameCommands() {}
}
