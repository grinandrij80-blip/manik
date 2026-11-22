package com.grinandrij80blip.manik;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Tripwires: a thin line between two points set by the maniac. A survivor who
 * touches it is stunned for 5 seconds (no damage) and the wire is used up.
 * The line and the floor knobs are vanilla block_display entities, created with
 * the summon command and removed when the wire fires or the round ends.
 */
public final class TripwireManager {
    public static final double MAX_LENGTH = 5.0;
    public static final int STUN_TICKS = 5 * 20;
    public static final int COOLDOWN_TICKS = 40 * 20;
    private static final double WIRE_THICKNESS = 0.025;
    /** How high the wire hangs above the floor on its black knobs. */
    private static final double FLOOR_LIFT = 0.12;
    private static final String ALL_TAG = "manik_tripwire";

    private record Anchor(ServerLevel level, Vec3 point, boolean floor) {}

    private static final class Wire {
        final ServerLevel level;
        final Vec3 a;
        final Vec3 b;
        final String tag;
        boolean floorA;
        boolean floorB;

        Wire(ServerLevel level, Vec3 a, Vec3 b, String tag) {
            this.level = level;
            this.a = a;
            this.b = b;
            this.tag = tag;
        }
    }

    private static final Map<UUID, Anchor> PENDING = new HashMap<>();
    private static final Map<UUID, Long> STUNNED_UNTIL = new HashMap<>();
    private static final List<Wire> WIRES = new ArrayList<>();
    private static int counter;

    public static void onUse(ServerPlayer player, ItemStack stack, Vec3 hit, Direction face) {
        if (!GameConfig.isRunning() || !player.getTags().contains("manik_killer")) {
            player.displayClientMessage(Component.literal("Розтяжку може ставити лише маніяк під час раунду."), true);
            return;
        }
        if (StaminaManager.isStunned(player)) {
            player.displayClientMessage(Component.literal("Ти оглушений і не можеш ставити розтяжку."), true);
            return;
        }
        if (player.getCooldowns().isOnCooldown(stack)) {
            player.displayClientMessage(Component.literal("Розтяжка перезаряджається."), true);
            return;
        }

        ServerLevel level = (ServerLevel) player.level();
        boolean floor = face == Direction.UP;
        Vec3 point = floor
                ? hit.add(0.0, FLOOR_LIFT, 0.0)
                : hit.add(face.getStepX() * 0.03, face.getStepY() * 0.03, face.getStepZ() * 0.03);

        Anchor first = PENDING.get(player.getUUID());
        if (first == null || first.level() != level) {
            PENDING.put(player.getUUID(), new Anchor(level, point, floor));
            player.displayClientMessage(Component.literal("Перша точка розтяжки задана. Тепер тисни на другу (до 5 блоків)."), true);
            return;
        }

        double length = first.point().distanceTo(point);
        if (length > MAX_LENGTH) {
            player.displayClientMessage(Component.literal("Занадто далеко: розтяжка до 5 блоків. Обери другу точку ближче."), true);
            return;
        }
        if (length < 0.5) {
            player.displayClientMessage(Component.literal("Занадто близько до першої точки."), true);
            return;
        }

        PENDING.remove(player.getUUID());
        String tag = "manik_tw_" + (++counter);
        spawnWire(level, first, new Anchor(level, point, floor), tag, player);
        Wire wire = new Wire(level, first.point(), point, tag);
        wire.floorA = first.floor();
        wire.floorB = floor;
        WIRES.add(wire);
        player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        player.displayClientMessage(Component.literal("Розтяжку встановлено."), true);
    }

    public static void onServerTick(TickEvent.ServerTickEvent.Post event) {
        if (WIRES.isEmpty() || !GameConfig.isRunning()) return;
        MinecraftServer server = event.server();
        if (server.getTickCount() % 4 == 0) {
            for (Wire wire : WIRES) showParticles(wire);
        }

        List<Wire> fired = new ArrayList<>();
        for (Wire wire : WIRES) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.level() != wire.level || !player.isAlive() || player.isSpectator()
                        || !player.getTags().contains("manik_survivor")) continue;
                AABB box = player.getBoundingBox().inflate(0.05);
                if (box.contains(wire.a) || box.clip(wire.a, wire.b).isPresent()) {
                    stun(player, server.getTickCount());
                    fired.add(wire);
                    break;
                }
            }
        }
        for (Wire wire : fired) {
            removeEntities(wire.level, wire.tag);
            WIRES.remove(wire);
        }
    }

    /** Faint dust along the wire (always visible) plus black dust at floor knobs. */
    private static void showParticles(Wire wire) {
        net.minecraft.core.particles.DustParticleOptions faint =
                new net.minecraft.core.particles.DustParticleOptions(0xE8E8E8, 0.45F);
        Vec3 d = wire.b.subtract(wire.a);
        int steps = Math.max(2, (int) Math.ceil(d.length() / 0.2));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = wire.a.add(d.scale(i / (double) steps));
            wire.level.sendParticles(faint, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
        if (wire.floorA || wire.floorB) {
            net.minecraft.core.particles.DustParticleOptions black =
                    new net.minecraft.core.particles.DustParticleOptions(0x050505, 1.4F);
            if (wire.floorA) knob(wire, black, wire.a);
            if (wire.floorB) knob(wire, black, wire.b);
        }
    }

    private static void knob(Wire wire, net.minecraft.core.particles.DustParticleOptions black, Vec3 end) {
        for (double h = 0.0; h <= FLOOR_LIFT; h += 0.06) {
            wire.level.sendParticles(black, end.x, end.y - h, end.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static void stun(ServerPlayer player, long now) {
        STUNNED_UNTIL.put(player.getUUID(), player.level().getGameTime() + STUN_TICKS);
        player.setSprinting(false);
        ShockManager.start(player, STUN_TICKS);
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, STUN_TICKS, 255, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, STUN_TICKS, 0, false, false, false));
        player.displayClientMessage(Component.literal("Ти потрапив на розтяжку! Оглушено на 5 секунд."), true);
    }

    /** A stunned survivor cannot jump either. */
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Long until = STUNNED_UNTIL.get(player.getUUID());
        if (until == null || player.level().getGameTime() >= until) return;
        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(movement.x(), 0.0, movement.z());
        player.setOnGround(true);
    }

    public static void clearAll(MinecraftServer server) {
        PENDING.clear();
        STUNNED_UNTIL.clear();
        WIRES.clear();
        for (ServerLevel level : server.getAllLevels()) {
            List<Entity> found = new ArrayList<>();
            for (Entity entity : level.getAllEntities()) {
                if (entity.getTags().contains(ALL_TAG)) found.add(entity);
            }
            for (Entity entity : found) entity.discard();
        }
    }

    private static void removeEntities(ServerLevel level, String tag) {
        List<Entity> found = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity.getTags().contains(tag)) found.add(entity);
        }
        for (Entity entity : found) entity.discard();
    }

    // ---- visuals -------------------------------------------------------------------

    private static void spawnWire(ServerLevel level, Anchor first, Anchor second, String tag, ServerPlayer reporter) {
        Vec3 a = first.point();
        Vec3 b = second.point();
        Vec3 d = b.subtract(a);
        double length = d.length();
        Vec3 dir = d.scale(1.0 / length);

        // Rotation taking the +X axis onto the wire direction.
        double qx = 0.0;
        double qy;
        double qz;
        double qw;
        if (dir.x < -0.9999) {
            qy = 1.0;
            qz = 0.0;
            qw = 0.0;
        } else {
            qy = -dir.z;
            qz = dir.y;
            qw = 1.0 + dir.x;
            double norm = Math.sqrt(qy * qy + qz * qz + qw * qw);
            qy /= norm;
            qz /= norm;
            qw /= norm;
        }

        // Centre the thin box on the line: shift by -R * (0, t/2, t/2).
        Vec3 offset = rotate(qx, qy, qz, qw, new Vec3(0.0, -WIRE_THICKNESS / 2, -WIRE_THICKNESS / 2));

        summon(level, a, tag, "minecraft:light_gray_stained_glass",
                new double[]{qx, qy, qz, qw}, new double[]{length, WIRE_THICKNESS, WIRE_THICKNESS},
                new double[]{offset.x, offset.y, offset.z}, reporter);

        // Black knobs under the ends that sit on the floor.
        for (Anchor end : new Anchor[]{first, second}) {
            if (!end.floor()) continue;
            Vec3 p = end.point();
            summon(level, new Vec3(p.x - 0.06, p.y - FLOOR_LIFT, p.z - 0.06), tag, "minecraft:black_concrete",
                    new double[]{0, 0, 0, 1}, new double[]{0.12, FLOOR_LIFT + 0.02, 0.12},
                    new double[]{0, 0, 0}, reporter);
        }
    }

    private static Vec3 rotate(double qx, double qy, double qz, double qw, Vec3 v) {
        // v' = v + 2w(u x v) + 2 u x (u x v)
        Vec3 u = new Vec3(qx, qy, qz);
        Vec3 uv = u.cross(v);
        Vec3 uuv = u.cross(uv);
        return v.add(uv.scale(2.0 * qw)).add(uuv.scale(2.0));
    }

    private static void summon(ServerLevel level, Vec3 pos, String tag, String block,
                               double[] rotation, double[] scale, double[] translation,
                               ServerPlayer reporter) {
        String nbt = String.format(Locale.ROOT,
                "{Tags:[\"%s\",\"%s\"],block_state:{Name:\"%s\"},transformation:{"
                        + "left_rotation:[%sf,%sf,%sf,%sf],right_rotation:[0f,0f,0f,1f],"
                        + "scale:[%sf,%sf,%sf],translation:[%sf,%sf,%sf]},view_range:2.0f}",
                ALL_TAG, tag, block,
                fmt(rotation[0]), fmt(rotation[1]), fmt(rotation[2]), fmt(rotation[3]),
                fmt(scale[0]), fmt(scale[1]), fmt(scale[2]),
                fmt(translation[0]), fmt(translation[1]), fmt(translation[2]));
        String command = String.format(Locale.ROOT, "summon minecraft:block_display %s %s %s %s",
                fmt(pos.x), fmt(pos.y), fmt(pos.z), nbt);
        MinecraftServer server = level.getServer();
        CommandSourceStack source = server.createCommandSourceStack().withLevel(level).withSuppressedOutput();
        try {
            server.getCommands().getDispatcher().execute(command, source);
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            reporter.displayClientMessage(Component.literal("Помилка візуалу розтяжки: " + e.getMessage()), false);
        }
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.5f", value);
    }

    private TripwireManager() {}
}
