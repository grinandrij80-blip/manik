package com.grinandrij80blip.manik;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Initial command layer for configuring and controlling a MANIK session.
 */
public final class GameCommands {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("game")
                .then(Commands.literal("start")
                        .executes(context -> start(context.getSource())))
                .then(Commands.literal("stop")
                        .executes(context -> stop(context.getSource())))
                .then(Commands.literal("status")
                        .executes(context -> status(context.getSource())))
                .then(Commands.literal("killer")
                        .then(Commands.argument("number", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 100))
                                .executes(context -> setKillers(
                                        context.getSource(),
                                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "number")))))
                .then(Commands.literal("menu")
                        .then(Commands.literal("generators")
                                .then(Commands.argument("number", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 100))
                                        .executes(context -> setGenerators(
                                                context.getSource(),
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

        GameConfig.setRunning(true);
        source.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("MANIK: ігрову сесію розпочато. Гравців: " + players
                        + ", маніяків: " + killers + ", генераторів: " + GameConfig.generators() + "."),
                false);
        return 1;
    }

    private static int stop(CommandSourceStack source) {
        if (!GameConfig.isRunning()) {
            source.sendFailure(Component.literal("Гра MANIK зараз не запущена."));
            return 0;
        }
        GameConfig.setRunning(false);
        source.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("MANIK: ігрову сесію зупинено. Налаштування збережено."),
                false);
        return 1;
    }

    private static int status(CommandSourceStack source) {
        String state = GameConfig.isRunning() ? "запущена" : "зупинена";
        source.sendSuccess(() -> Component.literal("MANIK: " + state
                + " | маніяків: " + GameConfig.killers()
                + " | генераторів: " + GameConfig.generators()), false);
        return 1;
    }

    private GameCommands() {
    }
}
