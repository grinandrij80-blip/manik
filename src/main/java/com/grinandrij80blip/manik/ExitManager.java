package com.grinandrij80blip.manik;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Escape sequence of a round:
 * 1. A survivor pulls the lever, which opens special doors 2.
 * 2. Behind them is a flare crate; carried into an exit zone and used, it fires flares for 60 s.
 * 3. When the helicopter arrives, every living survivor standing in an exit zone escapes.
 * The round ends when no survivor is left on the map (everyone escaped or died).
 */
public final class ExitManager {
    public static final int COUNTDOWN_TICKS = 60 * 20;
    /** Half of the zone side: the zone is 10 x 10 blocks, centred on the exit block. */
    public static final double ZONE_HALF = 5.0;

    public static final int PHASE_NONE = 0;
    public static final int PHASE_COUNTDOWN = 1;
    public static final int PHASE_OPEN = 2;
    private static final int ARRIVED_SHOW_TICKS = 100;

    private static int phase = PHASE_NONE;
    private static int ticksLeft;
    private static boolean doorsOpen;
    private static boolean launched;
    private static ServerLevel level;
    private static List<BlockPos> exits = new ArrayList<>();
    private static Vec3 flareSite = Vec3.ZERO;
    private static boolean rocketErrorShown;
    private static final Set<UUID> SURVIVORS = new HashSet<>();
    private static final Set<UUID> ESCAPED = new HashSet<>();
    private static final Set<UUID> DEAD = new HashSet<>();

    public static void beginRound(ServerLevel roundLevel, Collection<UUID> survivors) {
        phase = PHASE_NONE;
        ticksLeft = 0;
        doorsOpen = false;
        launched = false;
        rocketErrorShown = false;
        level = roundLevel;
        exits = new ArrayList<>();
        SURVIVORS.clear();
        ESCAPED.clear();
        DEAD.clear();
        SURVIVORS.addAll(survivors);
    }

    public static boolean isDoorsOpen(ServerLevel checkLevel) {
        return doorsOpen && GameConfig.isRunning() && level == checkLevel;
    }

    /** Lever pulled: opens special doors 2. Returns an error message or null. */
    public static String pullLever(ServerPlayer player) {
        if (!GameConfig.isRunning() || level == null) return "Гра не запущена.";
        if (doorsOpen) return "Спеціальні двері 2 уже відчинено.";
        doorsOpen = true;
        player.level().getServer().getPlayerList().broadcastSystemMessage(Component.literal(
                "MANIK: важіль увімкнено! Спеціальні двері 2 відчинено. Знайдіть ящик сигнальних ракет і віднесіть його в зону виходу."),
                false);
        return null;
    }

    /** Crate used. Returns an error message or null when the flares have been launched. */
    public static String launchFlares(ServerPlayer player) {
        if (!GameConfig.isRunning() || level == null) return "Гра не запущена.";
        if (!SURVIVORS.contains(player.getUUID())) return "Сигнальні ракети можуть запускати лише виживші.";
        if (launched) return "Сигнал уже запущено.";
        exits = SpawnPointManager.get(level, SpawnPointManager.Type.EXIT);
        if (exits.isEmpty()) return "На карті немає блока виходу.";
        BlockPos nearest = zoneAt(player);
        if (nearest == null) return "Запускати ракети треба в зоні виходу.";

        launched = true;
        flareSite = new Vec3(nearest.getX() + 0.5, nearest.getY(), nearest.getZ() + 0.5);
        phase = PHASE_COUNTDOWN;
        ticksLeft = COUNTDOWN_TICKS;
        MinecraftServer server = player.level().getServer();
        server.getPlayerList().broadcastSystemMessage(Component.literal(
                "MANIK: запущено сигнальні ракети! Вертоліт прилетить через 60 секунд. Ставай у зелену зону!"), false);
        syncAll(server);
        return null;
    }

    public static void onServerTick(TickEvent.ServerTickEvent.Post event) {
        MinecraftServer server = event.server();
        if (level == null) return;
        if (!GameConfig.isRunning()) {
            reset(server);
            return;
        }
        long tick = server.getTickCount();

        if (phase == PHASE_COUNTDOWN) {
            ticksLeft--;
            if (tick % 8 == 0) showZones();
            if (tick % 5 == 0) flareParticles();
            if (tick % 30 == 0) launchRocket(server);
            if (ticksLeft <= 200 && tick % 8 == 0) {
                Sfx.at(level, flareSite.x, flareSite.y + 10, flareSite.z,
                        SoundEvents.ENDER_DRAGON_FLAP, 3.0F + (200 - ticksLeft) / 40.0F, 0.6F);
            }
            if (ticksLeft <= 0) arrive(server);
        } else if (phase == PHASE_OPEN) {
            if (--ticksLeft <= 0) {
                phase = PHASE_NONE;
                syncAll(server);
            }
        }
        if (phase != PHASE_NONE && tick % 20 == 0) syncAll(server);

        int alive = 0;
        for (UUID id : new ArrayList<>(SURVIVORS)) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null || ESCAPED.contains(id) || DEAD.contains(id)) continue;
            if (!player.isAlive() || player.isDeadOrDying()) {
                DEAD.add(id);
                continue;
            }
            alive++;
        }

        if (!SURVIVORS.isEmpty() && alive == 0) {
            StringBuilder names = new StringBuilder();
            for (UUID id : ESCAPED) {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p != null) {
                    if (names.length() > 0) names.append(", ");
                    names.append(p.getName().getString());
                }
            }
            String text = "MANIK: раунд завершено. Втекли: " + ESCAPED.size()
                    + (names.length() > 0 ? " (" + names + ")" : "")
                    + ", загинули: " + DEAD.size() + ".";
            GameCommands.endRound(server, text);
        }
    }

    /** The helicopter arrives: living survivors standing in an exit zone escape. */
    private static void arrive(MinecraftServer server) {
        phase = PHASE_OPEN;
        ticksLeft = ARRIVED_SHOW_TICKS;
        server.getPlayerList().broadcastSystemMessage(Component.literal("MANIK: вертоліт прилетів!"), false);
        for (UUID id : new ArrayList<>(SURVIVORS)) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null || ESCAPED.contains(id) || DEAD.contains(id)) continue;
            if (!player.isAlive() || player.isDeadOrDying()) continue;
            if (zoneAt(player) != null) {
                ESCAPED.add(id);
                player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
                player.sendSystemMessage(Component.literal("Ти втік на вертольоті! Перемога!"));
                server.getPlayerList().broadcastSystemMessage(
                        Component.literal("MANIK: " + player.getName().getString() + " втік!"), false);
            }
        }
        syncAll(server);
    }

    /** The exit block whose zone the player is standing in, or null. */
    private static BlockPos zoneAt(ServerPlayer player) {
        if (player.level() != level) return null;
        for (BlockPos exit : exits) {
            if (Math.abs(player.getX() - (exit.getX() + 0.5)) <= ZONE_HALF
                    && Math.abs(player.getZ() - (exit.getZ() + 0.5)) <= ZONE_HALF
                    && player.getY() >= exit.getY() - 1.0 && player.getY() < exit.getY() + 2.0) {
                return exit;
            }
        }
        return null;
    }

    private static void showZones() {
        DustParticleOptions green = new DustParticleOptions(0x22DD55, 1.6F);
        int cells = (int) (ZONE_HALF * 2);
        for (BlockPos exit : exits) {
            for (int i = 0; i < cells; i++) {
                for (int j = 0; j < cells; j++) {
                    double x = exit.getX() + 0.5 - ZONE_HALF + 0.5 + i;
                    double z = exit.getZ() + 0.5 - ZONE_HALF + 0.5 + j;
                    level.sendParticles(green, x, exit.getY() + 0.1, z, 1, 0.0, 0.0, 0.0, 0.0);
                }
            }
        }
    }

    /** Rising firework sparks and bursts over the zone. */
    private static void flareParticles() {
        for (int c = 0; c < 3; c++) {
            double x = flareSite.x + (level.getRandom().nextDouble() - 0.5) * 6.0;
            double z = flareSite.z + (level.getRandom().nextDouble() - 0.5) * 6.0;
            for (int h = 1; h <= 30; h += 3) {
                level.sendParticles(ParticleTypes.FIREWORK, x, flareSite.y + h, z, 2, 0.12, 0.5, 0.12, 0.01);
            }
            if (level.getRandom().nextInt(6) == 0) {
                level.sendParticles(ParticleTypes.FIREWORK, x, flareSite.y + 30, z, 70, 0.6, 0.6, 0.6, 0.35);
            }
        }
    }

    private static final int[][] ROCKET_COLORS = {
            {16711680, 16755200}, {65280, 16777215}, {255, 65535}, {16711935, 16776960}, {16744192, 16711680}
    };

    private static void launchRocket(MinecraftServer server) {
        int[] colors = ROCKET_COLORS[level.getRandom().nextInt(ROCKET_COLORS.length)];
        double x = flareSite.x + (level.getRandom().nextDouble() - 0.5) * 6.0;
        double z = flareSite.z + (level.getRandom().nextDouble() - 0.5) * 6.0;
        String command = String.format(Locale.ROOT,
                "summon minecraft:firework_rocket %.2f %.2f %.2f {LifeTime:32,FireworksItem:{id:\"minecraft:firework_rocket\",count:1,"
                        + "components:{\"minecraft:fireworks\":{flight_duration:2,explosions:[{shape:\"large_ball\","
                        + "colors:[I;%d,%d],fade_colors:[I;16777215],has_trail:true,has_twinkle:true}]}}}}",
                x, flareSite.y + 0.5, z, colors[0], colors[1]);
        try {
            CommandSourceStack source = server.createCommandSourceStack().withLevel(level).withSuppressedOutput();
            server.getCommands().getDispatcher().execute(command, source);
        } catch (CommandSyntaxException e) {
            if (!rocketErrorShown) {
                rocketErrorShown = true;
                server.getPlayerList().broadcastSystemMessage(Component.literal(
                        "Помилка ракет (повідом розробника): " + e.getMessage()), false);
            }
        }
    }

    private static void syncAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ManikNetwork.sendExit(player, phase, ticksLeft);
        }
    }

    /** Clears the escape phase and returns escaped players to normal game mode. */
    public static void reset(MinecraftServer server) {
        for (UUID id : ESCAPED) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) player.setGameMode(server.getDefaultGameType());
        }
        ESCAPED.clear();
        DEAD.clear();
        SURVIVORS.clear();
        exits = new ArrayList<>();
        phase = PHASE_NONE;
        ticksLeft = 0;
        doorsOpen = false;
        launched = false;
        level = null;
        syncAll(server);
    }

    private ExitManager() {}
}
