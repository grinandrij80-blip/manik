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
import net.minecraftforge.event.RegisterCommandsEvent;

public final class GameCommands {
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
        if (GameConfig.isRunning()) {
            source.sendFailure(Component.literal("Зупини гру через /game stop перед зміною налаштувань."));
            return 0;
        }
        GameConfig.setKillers(number);
        source.sendSuccess(() -> Component.literal("Кількість маніяків: " + number), true);
        return 1;
    }

    private static int setGenerators(CommandSourceStack source, int number) {
        if (GameConfig.isRunning()) {
            source.sendFailure(Component.literal("Зупини гру через /game stop перед зміною налаштувань."));
            return 0;
        }
        GameConfig.setGenerators(number);
        source.sendSuccess(() -> Component.literal("Кількість генераторів: " + number), true);
        return 1;
    }

    private static int start(CommandSourceStack source) {
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
        int requiredCanisterSpawns = GameConfig.generators() * 2 + 3;
        int availableItemSpawns = SpawnPointManager.get(level, SpawnPointManager.Type.ITEM).size();
        if (availableItemSpawns < requiredCanisterSpawns) {
            source.sendFailure(Component.literal("Недостатньо жовтих точок появи предметів для каністр: потрібно "
                    + requiredCanisterSpawns + ", є " + availableItemSpawns + ". Додай ще "
                    + (requiredCanisterSpawns - availableItemSpawns) + " точок."));
            return 0;
        }
        if (SpawnPointManager.get(level, SpawnPointManager.Type.GENERATOR).size() < GameConfig.generators()) {
            int missing = GameConfig.generators() - SpawnPointManager.get(level, SpawnPointManager.Type.GENERATOR).size();
            source.sendFailure(Component.literal("Недостатньо синіх точок появи генераторів: потрібно ще " + missing + "."));
            return 0;
        }
        if (killerSpawns.size() < killers) {
            source.sendFailure(Component.literal("Недостатньо червоних точок спавну: потрібно ще " + (killers - killerSpawns.size()) + "."));
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
        for (int i = 0; i < shuffledPlayers.size(); i++) {
            ServerPlayer player = shuffledPlayers.get(i);
            boolean killer = i < killers;
            BlockPos spawn = killer ? killerSpawns.get(i) : survivorSpawns.get(i - killers);
            player.removeTag("manik_killer");
            player.removeTag("manik_survivor");
            player.addTag(killer ? "manik_killer" : "manik_survivor");
            player.teleportTo(level, spawn.getX() + 0.5, spawn.getY() + 1.0, spawn.getZ() + 0.5,
                    Set.of(), player.getYRot(), player.getXRot(), true);
            player.sendSystemMessage(Component.literal(killer ? "Твоя роль: МАНІЯК" : "Твоя роль: ВИЖИВШИЙ"));
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
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            player.removeTag("manik_killer");
            player.removeTag("manik_survivor");
        }
        source.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("MANIK: ігрову сесію зупинено. Налаштування збережено."), false);
        return 1;
    }

    private static int status(CommandSourceStack source) {
        String state = GameConfig.isRunning() ? "запущена" : "зупинена";
        source.sendSuccess(() -> Component.literal("MANIK: " + state
                + " | маніяків: " + GameConfig.killers()
                + " | генераторів: " + GameConfig.generators()), false);
        return 1;
    }

    private GameCommands() {}
}