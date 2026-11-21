package com.grinandrij80blip.manik;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
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

    public static boolean areAllGeneratorsReady(ServerLevel level) {
        if (!GameConfig.isRunning() || activeLevel != level || ACTIVE_GENERATORS.isEmpty()) return false;
        for (BlockPos pos : ACTIVE_GENERATORS) {
            if (!level.getBlockState(pos).is(ManikBlocks.GENERATOR.get())
                    || !GeneratorManager.isGeneratorReady(level, pos)) return false;
        }
        return true;
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("game")
                .then(Commands.literal("start").executes(context -> start(context.getSource())))
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

    private static int start(CommandSourceStack source) {
        GameConfig.load(source.getLevel());
        if (GameConfig.isRunning()) {
            source.sendFailure(Component.literal("Гра MANIK уже запущена."));
            return 0;
        }
        int players = source.getServer().getPlayerList().getPlayers().size();
        int killers = GameConfig.killers();
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
        if (availableGenerators < GameConfig.generators()) {
            source.sendFailure(Component.literal("Недостатньо синіх точок появи генераторів: потрібно ще "
                    + (GameConfig.generators() - availableGenerators) + "."));
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

        List<ServerPlayer> shuffledPlayers = new ArrayList<>(source.getServer().getPlayerList().getPlayers());
        Collections.shuffle(shuffledPlayers);
        Collections.shuffle(killerSpawns);
        Collections.shuffle(survivorSpawns);

        ACTIVE_GENERATORS.clear();
        ACTIVE_ITEMS.clear();
        GloveManager.resetRound();
        TrapManager.beginRound(level);
        List<BlockPos> generatorSpawns = SpawnPointManager.get(level, SpawnPointManager.Type.GENERATOR);
        Collections.shuffle(generatorSpawns);
        for (int i = 0; i < GameConfig.generators(); i++) {
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
                    level, itemPos.getX() + 0.5, itemPos.getY() + 1.02, itemPos.getZ() + 0.5,
                    new net.minecraft.world.item.ItemStack(ManikItems.CANISTER.get()));
            if (level.addFreshEntity(canister)) ACTIVE_ITEMS.add(canister.getUUID());
        }

        // Remaining item points receive only utility loot: 50% crowbar, 50% medkit. Traps are issued to killers.
        for (int i = canisterCount; i < itemSpawns.size(); i++) {
            BlockPos itemPos = itemSpawns.get(i);
            int utilityRoll = java.util.concurrent.ThreadLocalRandom.current().nextInt(2);
            net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(
                    utilityRoll == 0 ? ManikItems.CROWBAR.get() : ManikItems.MEDKIT.get());
            net.minecraft.world.entity.item.ItemEntity utilityItem = new net.minecraft.world.entity.item.ItemEntity(
                    level, itemPos.getX() + 0.5, itemPos.getY() + 1.02, itemPos.getZ() + 0.5, stack);
            if (level.addFreshEntity(utilityItem)) ACTIVE_ITEMS.add(utilityItem.getUUID());
        }

        for (int i = 0; i < shuffledPlayers.size(); i++) {
            ServerPlayer player = shuffledPlayers.get(i);
            boolean killer = i < killers;
            BlockPos spawn = killer ? killerSpawns.get(i) : survivorSpawns.get(i - killers);
            player.removeTag("manik_killer");
            player.removeTag("manik_survivor");
            player.addTag(killer ? "manik_killer" : "manik_survivor");

            // A trap is a role item, not map loot: remove any leftover copy,
            // then give exactly one directly to each killer.
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                if (player.getInventory().getItem(slot).is(ManikBlocks.TRAP_ITEM.get())
                        || player.getInventory().getItem(slot).is(ManikItems.MARK_GLOVE.get())) {
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
                            "Тобі видано рукавицю: влучання знімає 4 HP (2 серця), третє влучання вибуває з раунду. Перезарядка: 12 секунд."));
                } else {
                    player.sendSystemMessage(Component.literal("Немає вільного місця для рукавиці! Звільни слот."));
                }
            }
            player.teleportTo(level, spawn.getX() + 0.5, spawn.getY() + 1.0, spawn.getZ() + 0.5,
                    Set.of(), player.getYRot(), player.getXRot(), true);
            player.sendSystemMessage(Component.literal(killer ? "Твоя роль: МАНІЯК" : "Твоя роль: ВИЖИВШИЙ"));
            StaminaManager.beginRound(player);
        }

        GameConfig.setRunning(true);
        source.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("MANIK: гру розпочато! Маніяків: " + killers
                        + ", виживших: " + (players - killers) + "."), false);
        return 1;
    }

    private static int stop(CommandSourceStack source) {
        if (!GameConfig.isRunning()) {
            source.sendFailure(Component.literal("Гра MANIK зараз не запущена."));
            return 0;
        }
        GameConfig.setRunning(false);
        GloveManager.resetRound();
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
        StaminaManager.endRound(source.getServer().getPlayerList().getPlayers());
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            player.removeTag("manik_killer");
            player.removeTag("manik_survivor");
        }
        source.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("MANIK: гру зупинено. Генератори й ігрові предмети прибрано, точки спавну збережено, ролі скинуто."),
                false);
        return 1;
    }

    private static int status(CommandSourceStack source) {
        GameConfig.load(source.getLevel());
        String state = GameConfig.isRunning() ? "запущена" : "зупинена";
        source.sendSuccess(() -> Component.literal("MANIK: " + state
                + " | маніяків: " + GameConfig.killers()
                + " | генераторів: " + GameConfig.generators()), false);
        return 1;
    }

    private GameCommands() {}
}
