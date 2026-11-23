package com.grinandrij80blip.manik;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.Result;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The flare crate: it stands at a crate spawn, a survivor picks it up with right click
 * and carries it with both hands (not in the inventory), as slowly as when sneaking.
 * Right click puts it back on the floor; right click inside an exit zone launches the flares.
 */
public final class CarryManager {
    private static final Identifier SLOW_ID = Identifier.parse("manik:carry_slow");
    private static final double SLOW_AMOUNT = -0.7; // about the speed of sneaking

    private static UUID carrier;
    private static ServerLevel level;
    private static final Set<BlockPos> CRATE_BLOCKS = new HashSet<>();

    // ---- round lifecycle ---------------------------------------------------------

    public static void beginRound(ServerLevel roundLevel) {
        level = roundLevel;
        carrier = null;
        CRATE_BLOCKS.clear();
        List<BlockPos> spawns = SpawnPointManager.get(roundLevel, SpawnPointManager.Type.CRATE);
        if (spawns.isEmpty()) {
            roundLevel.getServer().getPlayerList().broadcastSystemMessage(Component.literal(
                    "MANIK: на карті немає точки появи ящика сигнальних ракет!"), false);
            return;
        }
        BlockPos spawn = spawns.get(roundLevel.getRandom().nextInt(spawns.size()));
        roundLevel.setBlockAndUpdate(spawn, ManikBlocks.FLARE_CRATE.get().defaultBlockState());
        CRATE_BLOCKS.add(spawn.immutable());
    }

    /** Removes every crate and puts the spawn markers back. */
    public static void endRound(ServerLevel roundLevel, net.minecraft.server.MinecraftServer server) {
        if (carrier != null) {
            ServerPlayer p = server.getPlayerList().getPlayer(carrier);
            if (p != null) setCarrying(p, false);
        }
        carrier = null;
        if (roundLevel != null) {
            for (BlockPos pos : CRATE_BLOCKS) {
                if (roundLevel.getBlockState(pos).is(ManikBlocks.FLARE_CRATE.get())) {
                    roundLevel.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                }
            }
            for (BlockPos pos : SpawnPointManager.get(roundLevel, SpawnPointManager.Type.CRATE)) {
                roundLevel.setBlockAndUpdate(pos, ManikBlocks.CRATE_SPAWN.get().defaultBlockState());
            }
        }
        CRATE_BLOCKS.clear();
        level = null;
    }

    // ---- picking up / putting down -----------------------------------------------

    public static void pickUp(ServerPlayer player, BlockPos pos) {
        if (!GameConfig.isRunning() || level == null) {
            player.displayClientMessage(Component.literal("Ящик можна брати лише під час раунду."), true);
            return;
        }
        if (!player.getTags().contains("manik_survivor")) {
            player.displayClientMessage(Component.literal("Ящик сигнальних ракет можуть брати лише виживші."), true);
            return;
        }
        if (carrier != null) return;
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        CRATE_BLOCKS.remove(pos);
        carrier = player.getUUID();
        setCarrying(player, true);
        player.displayClientMessage(Component.literal(
                "Ти несеш ящик двома руками. ПКМ у зоні виходу: запустити ракети. ПКМ деінде: поставити. Shift+ПКМ: поставити в зоні."), true);
    }

    private static void putDown(ServerPlayer player, BlockPos target) {
        level.setBlockAndUpdate(target, ManikBlocks.FLARE_CRATE.get().defaultBlockState());
        CRATE_BLOCKS.add(target.immutable());
        carrier = null;
        setCarrying(player, false);
        player.displayClientMessage(Component.literal("Ти поставив ящик."), true);
    }

    /** Tells everybody who carries the crate, so they can see the pose and the crate. */
    private static void broadcastCarrier(net.minecraft.server.MinecraftServer server, int entityId) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            ManikNetwork.sendCarrier(p, entityId);
        }
    }

    private static void setCarrying(ServerPlayer player, boolean carrying) {
        broadcastCarrier(player.level().getServer(), carrying ? player.getId() : -1);
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SLOW_ID);
            if (carrying) {
                speed.addOrUpdateTransientModifier(new AttributeModifier(
                        SLOW_ID, SLOW_AMOUNT, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }
        ManikNetwork.sendCarry(player, carrying);
    }

    private static boolean canPlace(ServerPlayer player, BlockPos target) {
        return level.getBlockState(target).canBeReplaced()
                && !player.getBoundingBox().intersects(new AABB(target));
    }

    /** A free floor spot a little in front of the player (or around them). */
    private static BlockPos findSpotNear(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0.0, look.z);
        forward = forward.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : forward.normalize();
        double[][] offsets = {{1.3, 0}, {1.3, 1.2}, {1.3, -1.2}, {-1.3, 0}, {0, 1.3}, {0, -1.3}};
        Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
        for (double[] o : offsets) {
            Vec3 base = player.position().add(forward.scale(o[0])).add(side.scale(o[1]));
            for (int dy = 1; dy >= -3; dy--) {
                BlockPos pos = BlockPos.containing(base.x, player.getY() + dy, base.z);
                if (!level.getBlockState(pos.below()).isAir() && canPlace(player, pos)) return pos;
            }
        }
        return null;
    }

    private static void handle(ServerPlayer player, BlockPos clicked, Direction face) {
        if (!player.isShiftKeyDown() && ExitManager.isInExitZone(player)) {
            String error = ExitManager.launchFlares(player);
            if (error != null) {
                player.displayClientMessage(Component.literal(error), true);
            } else {
                // The crate is used up.
                carrier = null;
                setCarrying(player, false);
            }
            return;
        }
        BlockPos target = clicked != null && face != null ? clicked.relative(face) : null;
        if (target == null || !canPlace(player, target)) target = findSpotNear(player);
        if (target == null) {
            player.displayClientMessage(Component.literal("Тут немає місця, щоб поставити ящик."), true);
            return;
        }
        putDown(player, target);
    }

    private static boolean isCarrying(Player player) {
        if (player.level().isClientSide()) return CarryState.isCarrying();
        return carrier != null && carrier.equals(player.getUUID());
    }

    // ---- events ------------------------------------------------------------------

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!isCarrying(player)) return;
        event.setUseBlock(Result.DENY);
        event.setUseItem(Result.DENY);
        if (player.level().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND) return;
        if (player instanceof ServerPlayer sp) handle(sp, event.getPos(), event.getFace());
    }

    public static boolean onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (!isCarrying(player)) return false;
        if (!player.level().isClientSide() && event.getHand() == InteractionHand.MAIN_HAND
                && player instanceof ServerPlayer sp) {
            handle(sp, null, null);
        }
        return true;
    }

    /** Both hands are busy: no attacking while carrying. */
    public static boolean onAttack(AttackEntityEvent event) {
        return isCarrying(event.getEntity());
    }

    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof ServerPlayer player) || carrier == null
                || !carrier.equals(player.getUUID())) return;
        if (!player.isAlive() || !GameConfig.isRunning()) {
            dropAt(player);
            return;
        }
        player.setSprinting(false);
        if (player.tickCount % 20 == 0) broadcastCarrier(player.level().getServer(), player.getId());
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && carrier != null
                && carrier.equals(player.getUUID())) {
            dropAt(player);
        }
    }

    /** The carrier died or left: the crate is left on the floor where they were. */
    private static void dropAt(ServerPlayer player) {
        carrier = null;
        if (level != null && GameConfig.isRunning()) {
            BlockPos spot = player.blockPosition();
            List<BlockPos> candidates = new ArrayList<>();
            for (int dy = 0; dy <= 3; dy++) candidates.add(spot.above(dy));
            for (BlockPos pos : candidates) {
                if (level.getBlockState(pos).canBeReplaced()) {
                    level.setBlockAndUpdate(pos, ManikBlocks.FLARE_CRATE.get().defaultBlockState());
                    CRATE_BLOCKS.add(pos.immutable());
                    break;
                }
            }
        }
        setCarrying(player, false);
    }

    private CarryManager() {}
}
