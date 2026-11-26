package com.grinandrij80blip.manik;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.Result;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server logic of the two test-only maniacs: the Beekeeper (type 2) and the Fallen Angel (type 3),
 * plus the resurrected minion. None of this is reachable outside /test maniac.
 */
public final class ExtraManiacs {
    // ---- tuning ----
    public static final int FLIGHT_TICKS = 60;              // beekeeper wings: 3 s
    public static final int HIVE_ITEM_COOLDOWN = 110 * 20;
    public static final double HIVE_RADIUS = 3.0D;
    public static final int BEE_LIFETIME = 14 * 20;
    public static final double BEE_SPEED = 0.3D;
    public static final float BEE_HP_HIVE = 4.0F;
    public static final float BEE_HP_POWER = 6.0F;
    public static final float HONEY_DROP_CHANCE = 0.15F;
    public static final float SMOKER_DIRECT = 2.0F;
    public static final float SMOKER_POISON = 6.0F;
    public static final int POISON_INTERVAL = 25;

    public static final float BOW_DAMAGE = 8.0F;
    public static final int BOW_HIT_COOLDOWN = 10 * 20;
    public static final int BOW_MISS_COOLDOWN = 16;          // 0.8 s
    public static final double BOW_RANGE = 60.0D;
    public static final int CANDLE_ITEM_COOLDOWN = 60 * 20;
    public static final double ANGEL_FLIGHT_TICKS = 50.0D;   // 2.5 s of flight
    public static final double ANGEL_REGEN_PER_TICK = 50.0D / 80.0D; // full in 4 s
    public static final int ANGEL_STUN_TICKS = 5 * 20;
    public static final int DAGGER_COOLDOWN = 20 * 20;
    public static final float DAGGER_DAMAGE = 4.0F;
    public static final double MINION_SCALE = 0.9D;

    private static long serverTicks;

    // ---- type helpers ----
    public static boolean isMinion(net.minecraft.world.entity.player.Player p) {
        return p.getTags().contains("manik_minion");
    }

    private static boolean activeManiac(ServerPlayer p, int type) {
        return GameConfig.isRunning() && p.isAlive() && p.getTags().contains("manik_killer")
                && ManiacTypes.get(p) == type;
    }

    private static boolean isTarget(ServerPlayer p) {
        return p.isAlive() && !p.isSpectator() && p.getTags().contains("manik_survivor");
    }

    public static int total1(int type) {
        return type == ManiacTypes.BEEKEEPER ? 80 * 20 : 70 * 20;
    }

    public static int total2(int type) {
        return type == ManiacTypes.BEEKEEPER ? 100 * 20 : 600 * 20;
    }

    public static int castTicks(int type) {
        return type == ManiacTypes.BEEKEEPER ? 30 : 15 * 20;
    }

    // ---------------------------------------------------------------- poison
    private static final class Poison { float hp; long next; }
    private static final Map<UUID, Poison> POISON = new HashMap<>();

    /** Poison stacks sequentially: 1 HP every 25 ticks, never lethal. */
    public static void addPoison(ServerPlayer victim, float hp) {
        Poison p = POISON.computeIfAbsent(victim.getUUID(), k -> new Poison());
        if (p.hp <= 0) p.next = serverTicks + POISON_INTERVAL;
        p.hp += hp;
    }

    private static void tickPoison(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Poison>> it = POISON.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            Poison s = e.getValue();
            if (p == null || !p.isAlive() || p.isSpectator() || s.hp <= 0) { it.remove(); continue; }
            if (serverTicks < s.next) continue;
            s.next = serverTicks + POISON_INTERVAL;
            if (p.getHealth() > 1.0F) {
                p.hurt(p.damageSources().magic(), 1.0F);
            }
            s.hp -= 1.0F;
            if (p.level() instanceof ServerLevel lv) {
                lv.sendParticles(ParticleTypes.SNEEZE, p.getX(), p.getY() + 1.0, p.getZ(), 3, 0.3, 0.4, 0.3, 0.01);
            }
        }
    }

    public static void onSmokerHit(ServerPlayer attacker, ServerPlayer target) {
        addPoison(target, SMOKER_POISON);
        if (target.level() instanceof ServerLevel lv) {
            lv.sendParticles(ParticleTypes.LARGE_SMOKE, target.getX(), target.getY() + 1.0, target.getZ(),
                    25, 0.3, 0.5, 0.3, 0.02);
        }
        Sfx.at(target, SoundEvents.FIRE_EXTINGUISH, 1.0F, 0.8F);
        target.displayClientMessage(Component.literal("Дим! -2 HP і отрута на 6 HP."), true);
    }

    // ------------------------------------------------------------------ bees
    private static final class BeeState {
        Bee bee; float hp; boolean honey; long expire;
    }
    private static final Map<UUID, BeeState> BEES = new HashMap<>();

    private static void spawnBee(ServerLevel level, Vec3 pos, float hp, boolean honey) {
        Bee bee = new Bee(EntityType.BEE, level);
        bee.setPos(pos.x, pos.y, pos.z);
        bee.setNoAi(true);
        bee.setNoGravity(true);
        bee.setInvulnerable(true);
        bee.addTag("manik_bee");
        bee.setPersistenceRequired();
        level.addFreshEntity(bee);
        BeeState s = new BeeState();
        s.bee = bee; s.hp = hp; s.honey = honey; s.expire = serverTicks + BEE_LIFETIME;
        BEES.put(bee.getUUID(), s);
    }

    private static void tickBees(MinecraftServer server) {
        Iterator<Map.Entry<UUID, BeeState>> it = BEES.entrySet().iterator();
        while (it.hasNext()) {
            BeeState s = it.next().getValue();
            Bee bee = s.bee;
            if (bee.isRemoved()) { it.remove(); continue; }
            if (serverTicks >= s.expire || !GameConfig.isRunning()) { bee.discard(); it.remove(); continue; }

            ServerPlayer best = null;
            double bestD = Double.MAX_VALUE;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (!isTarget(p) || p.level() != bee.level()) continue;
                double d = p.distanceToSqr(bee);
                if (d < bestD) { bestD = d; best = p; }
            }
            if (best == null) continue;

            Vec3 to = best.position().add(0, best.getBbHeight() * 0.6, 0).subtract(bee.position());
            double dist = to.length();
            if (dist < 1.0D) {
                sting(s, best);
                bee.discard();
                it.remove();
                continue;
            }
            Vec3 step = to.scale(BEE_SPEED / dist);
            bee.move(MoverType.SELF, step);
            float yaw = (float) (Mth.atan2(-to.x, to.z) * (180.0D / Math.PI));
            bee.setYRot(yaw);
            bee.yBodyRot = yaw;
            bee.setYHeadRot(yaw);
        }
    }

    private static void sting(BeeState s, ServerPlayer target) {
        addPoison(target, s.hp);
        Sfx.at(target, SoundEvents.BEE_STING, 1.0F, 1.0F);
        target.displayClientMessage(Component.literal("Бджола вжалила! Отрута на " + (int) s.hp + " HP."), true);
        if (s.honey && target.getRandom().nextFloat() < HONEY_DROP_CHANCE && target.level() instanceof ServerLevel lv) {
            ItemEntity drop = new ItemEntity(lv, target.getX(), target.getY(), target.getZ(),
                    new ItemStack(ManikItems.HONEY_BOTTLE.get()));
            lv.addFreshEntity(drop);
        }
    }

    /** A survivor hits a bee: only the stun gun works, and it spends one durability. Always cancels. */
    public static void onBeeAttacked(ServerPlayer attacker, Entity bee) {
        ItemStack weapon = attacker.getMainHandItem();
        if (!attacker.getTags().contains("manik_survivor") || !weapon.is(ManikItems.STUN_GUN.get())) {
            attacker.displayClientMessage(Component.literal("Бджолу відбиває лише електрошокер."), true);
            return;
        }
        if (attacker.getCooldowns().isOnCooldown(weapon)) return;
        attacker.getCooldowns().addCooldown(weapon, 20);
        CrowbarItem.consumeUse(attacker, InteractionHand.MAIN_HAND, weapon);
        if (bee.level() instanceof ServerLevel lv) {
            lv.sendParticles(ParticleTypes.ELECTRIC_SPARK, bee.getX(), bee.getY(), bee.getZ(), 15, 0.2, 0.2, 0.2, 0.1);
        }
        Sfx.at(bee, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.5F, 2.0F);
        bee.discard();
        BEES.remove(bee.getUUID());
    }

    // ----------------------------------------------------------------- hives
    private record Hive(ServerLevel level, BlockPos pos, UUID owner) {}
    private static final List<Hive> HIVES = new ArrayList<>();

    public static void placeHive(ServerPlayer p, BlockPos pos, ItemStack stack) {
        if (!activeManiac(p, ManiacTypes.BEEKEEPER) || StaminaManager.isDisabled(p)) return;
        if (p.getCooldowns().isOnCooldown(stack)) return;
        ServerLevel level = (ServerLevel) p.level();
        if (!level.getBlockState(pos).isAir()
                || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
            p.displayClientMessage(Component.literal("Вулик ставиться на тверду поверхню."), true);
            return;
        }
        level.setBlock(pos, Blocks.BEE_NEST.defaultBlockState(), 3);
        HIVES.add(new Hive(level, pos.immutable(), p.getUUID()));
        p.getCooldowns().addCooldown(stack, HIVE_ITEM_COOLDOWN);
        Sfx.at(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BEEHIVE_ENTER, 1.0F, 0.8F);
        p.displayClientMessage(Component.literal("Міні-вулик поставлено. Перезарядка: 110 с."), true);
    }

    private static void tickHives(MinecraftServer server) {
        Iterator<Hive> it = HIVES.iterator();
        while (it.hasNext()) {
            Hive h = it.next();
            if (!h.level().getBlockState(h.pos()).is(Blocks.BEE_NEST)) { it.remove(); continue; }
            Vec3 c = Vec3.atCenterOf(h.pos());
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (!isTarget(p) || p.level() != h.level()) continue;
                if (p.position().distanceTo(c) <= HIVE_RADIUS) {
                    h.level().setBlock(h.pos(), Blocks.AIR.defaultBlockState(), 3);
                    spawnBee(h.level(), c.add(0, 0.6, 0), BEE_HP_HIVE, true);
                    spawnBee(h.level(), c.add(0.3, 0.8, 0.3), BEE_HP_HIVE, true);
                    Sfx.at(h.level(), c.x, c.y, c.z, SoundEvents.BEEHIVE_SHEAR, 1.0F, 0.7F);
                    p.displayClientMessage(Component.literal("Вулик розкрився! Бджоли!"), true);
                    it.remove();
                    break;
                }
            }
        }
    }

    // ---------------------------------------------------------------- honey
    public static void drinkHoney(ServerPlayer p, ItemStack stack) {
        Sfx.at(p, SoundEvents.HONEY_DRINK, 1.0F, 1.0F);
        if (p.getRandom().nextFloat() < 0.8F) {
            p.heal(4.0F);
            p.displayClientMessage(Component.literal("Солодкий мед: +4 HP."), true);
        } else {
            p.hurt(p.damageSources().magic(), 12.0F);
            p.displayClientMessage(Component.literal("Мед виявився отруєним: -12 HP!"), true);
        }
        if (!p.getAbilities().instabuild) stack.shrink(1);
    }

    // --------------------------------------------------------------- candles
    private static final class Candle {
        ServerLevel level; BlockPos pos; UUID owner; int lvl = 1; float burned; long next;
    }
    private static final Map<UUID, Candle> CANDLES = new HashMap<>();
    private static final Map<UUID, long[]> BLOW = new HashMap<>(); // player -> {tick opened}

    private static double candleRadius(int lvl) {
        return lvl == 1 ? 1.0D : lvl == 4 ? 3.0D : 2.0D;
    }

    private static float candleLimit(int lvl) {
        return lvl == 1 ? 4.0F : lvl == 4 ? Float.MAX_VALUE : 8.0F;
    }

    private static Candle candleAt(BlockPos pos) {
        for (Candle c : CANDLES.values()) if (c.pos.equals(pos)) return c;
        return null;
    }

    private static BlockState candleState(int lvl) {
        return Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true).setValue(CandleBlock.CANDLES, lvl);
    }

    public static void placeCandle(ServerPlayer p, BlockPos pos, ItemStack stack) {
        if (!activeManiac(p, ManiacTypes.ANGEL) || StaminaManager.isDisabled(p)) return;
        if (p.getCooldowns().isOnCooldown(stack)) return;
        if (CANDLES.containsKey(p.getUUID())) {
            p.displayClientMessage(Component.literal("Свічка вже стоїть: можна лише покращити її (ПКМ по ній)."), true);
            return;
        }
        ServerLevel level = (ServerLevel) p.level();
        if (!level.getBlockState(pos).isAir()
                || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
            p.displayClientMessage(Component.literal("Свічка ставиться на тверду поверхню."), true);
            return;
        }
        level.setBlock(pos, candleState(1), 3);
        Candle c = new Candle();
        c.level = level; c.pos = pos.immutable(); c.owner = p.getUUID(); c.next = serverTicks + 20;
        CANDLES.put(p.getUUID(), c);
        p.getCooldowns().addCooldown(stack, CANDLE_ITEM_COOLDOWN);
        Sfx.at(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.FIRECHARGE_USE, 0.8F, 1.2F);
        p.displayClientMessage(Component.literal("Свічку запалено (рівень 1). Перезарядка: 60 с."), true);
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel)) return;
        Candle c = candleAt(event.getPos());
        if (c == null) return;
        event.setUseBlock(Result.DENY);
        event.setUseItem(Result.DENY);
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        ItemStack stack = event.getItemStack();

        if (p.getUUID().equals(c.owner) && stack.is(ManikItems.CANDLE_ITEM.get())) {
            if (!activeManiac(p, ManiacTypes.ANGEL) || StaminaManager.isDisabled(p)) return;
            if (p.getCooldowns().isOnCooldown(stack)) {
                p.displayClientMessage(Component.literal("Свічка ще не перезарядилась."), true);
                return;
            }
            if (c.lvl >= 4) {
                p.displayClientMessage(Component.literal("Свічка вже на максимальному рівні."), true);
                return;
            }
            c.lvl++;
            c.burned = 0;
            c.level.setBlock(c.pos, candleState(c.lvl), 3);
            p.getCooldowns().addCooldown(stack, CANDLE_ITEM_COOLDOWN);
            Sfx.at(c.level, c.pos.getX() + 0.5, c.pos.getY() + 0.5, c.pos.getZ() + 0.5,
                    SoundEvents.FIRECHARGE_USE, 1.0F, 0.8F + c.lvl * 0.1F);
            p.displayClientMessage(Component.literal("Свічка покращена до рівня " + c.lvl + "."), true);
            return;
        }
        if (isTarget(p) && p.position().distanceTo(Vec3.atCenterOf(c.pos)) <= 4.5D) {
            BLOW.put(p.getUUID(), new long[]{serverTicks});
            ManikNetwork.sendBlowOpen(p);
        }
    }

    public static void onBlowDone(ServerPlayer p) {
        long[] open = BLOW.remove(p.getUUID());
        if (open == null || serverTicks - open[0] < 20 || !isTarget(p)) return;
        for (Candle c : new ArrayList<>(CANDLES.values())) {
            if (p.level() == c.level && p.position().distanceTo(Vec3.atCenterOf(c.pos)) <= 5.0D) {
                snuff(c, true);
                p.displayClientMessage(Component.literal("Ти загасив свічку!"), true);
                return;
            }
        }
    }

    private static void snuff(Candle c, boolean sound) {
        if (c.level.getBlockState(c.pos).is(Blocks.CANDLE)) c.level.setBlock(c.pos, Blocks.AIR.defaultBlockState(), 3);
        if (sound) Sfx.at(c.level, c.pos.getX() + 0.5, c.pos.getY() + 0.5, c.pos.getZ() + 0.5,
                SoundEvents.FIRE_EXTINGUISH, 1.0F, 1.0F);
        CANDLES.remove(c.owner);
    }

    public static boolean onBlockBreak(BlockEvent.BreakEvent event) {
        return candleAt(event.getPos()) != null;
    }

    private static void tickCandles(MinecraftServer server) {
        for (Candle c : new ArrayList<>(CANDLES.values())) {
            if (!c.level.getBlockState(c.pos).is(Blocks.CANDLE)) { CANDLES.remove(c.owner); continue; }
            Vec3 center = Vec3.atCenterOf(c.pos);
            if (serverTicks % 4 == 0) {
                c.level.sendParticles(ParticleTypes.FLAME, center.x, center.y - 0.1 + c.lvl * 0.05, center.z, 1, 0.05, 0.02, 0.05, 0.0);
            }
            if (serverTicks < c.next) continue;
            boolean fast = c.lvl >= 3;
            c.next = serverTicks + (fast ? 15 : 20);
            float dmg = fast ? 1.0F : 0.5F;
            double r = candleRadius(c.lvl);
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (!isTarget(p) || p.level() != c.level) continue;
                if (Math.abs(p.getY() - c.pos.getY()) > 2.0D) continue;
                double dx = p.getX() - center.x, dz = p.getZ() - center.z;
                if (dx * dx + dz * dz > r * r) continue;
                p.hurt(c.level.damageSources().inFire(), dmg);
                c.burned += dmg;
            }
            if (c.burned >= candleLimit(c.lvl)) {
                snuff(c, true);
            }
        }
    }

    // ------------------------------------------------------------------- bow
    public static void shootBow(ServerPlayer p, ItemStack stack) {
        if (!activeManiac(p, ManiacTypes.ANGEL) || StaminaManager.isDisabled(p)) return;
        if (p.getCooldowns().isOnCooldown(stack)) return;
        ServerLevel level = (ServerLevel) p.level();
        Vec3 from = p.getEyePosition();
        Vec3 end = from.add(p.getLookAngle().scale(BOW_RANGE));
        var block = level.clip(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();

        ServerPlayer hit = null;
        double best = Double.MAX_VALUE;
        Vec3 hitPos = null;
        for (ServerPlayer t : level.getServer().getPlayerList().getPlayers()) {
            if (t == p || !isTarget(t) || t.level() != level) continue;
            AABB box = t.getBoundingBox().inflate(0.3);
            var r = box.clip(from, end);
            if (r.isPresent()) {
                double d = from.distanceToSqr(r.get());
                if (d < best) { best = d; hit = t; hitPos = r.get(); }
            }
        }
        Vec3 stop = hitPos != null ? hitPos : end;
        Vec3 dir = stop.subtract(from);
        int steps = (int) Math.max(1, dir.length() * 2);
        for (int i = 1; i <= steps; i++) {
            Vec3 pt = from.add(dir.scale(i / (double) steps));
            level.sendParticles(ParticleTypes.ASH, pt.x, pt.y, pt.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
        Sfx.at(p, SoundEvents.ARROW_SHOOT, 1.0F, 1.4F);
        if (hit != null) {
            hit.hurt(p.damageSources().generic(), BOW_DAMAGE);
            Sfx.at(hit, SoundEvents.ARROW_HIT_PLAYER, 1.0F, 0.8F);
            hit.displayClientMessage(Component.literal("Чорне перо: -8 HP."), true);
            p.getCooldowns().addCooldown(stack, BOW_HIT_COOLDOWN);
            p.displayClientMessage(Component.literal("Влучив! Лук перезаряджається 10 с."), true);
        } else {
            p.getCooldowns().addCooldown(stack, BOW_MISS_COOLDOWN);
        }
    }

    // ---------------------------------------------------------------- powers
    private static final Map<UUID, Long> FLY_UNTIL = new HashMap<>();
    private static final Set<UUID> FLYING_BY_US = new HashSet<>();

    public static int flightLeft(ServerPlayer p) {
        Long u = FLY_UNTIL.get(p.getUUID());
        return u == null ? 0 : (int) Math.max(0L, u - p.level().getGameTime());
    }

    public static boolean isPower1Active(ServerPlayer p) {
        return ManiacTypes.get(p) == ManiacTypes.BEEKEEPER && flightLeft(p) > 0;
    }

    public static void power1(ServerPlayer p) {
        if (ManiacTypes.get(p) == ManiacTypes.BEEKEEPER) {
            FLY_UNTIL.put(p.getUUID(), p.level().getGameTime() + FLIGHT_TICKS);
            Sfx.at(p, SoundEvents.BEE_LOOP, 1.0F, 1.2F);
        } else {
            for (ServerPlayer t : p.level().getServer().getPlayerList().getPlayers()) {
                if (!isTarget(t)) continue;
                t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20, 0, false, false, false));
                t.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20, 2, false, false, true));
                Sfx.toPlayer(t, SoundEvents.PHANTOM_SWOOP, 1.0F, 0.6F);
            }
            Sfx.at(p, SoundEvents.PHANTOM_SWOOP, 1.0F, 0.6F);
        }
    }

    /** False (with a message) when the cast must be refused without spending the cooldown. */
    public static boolean canCast2(ServerPlayer p) {
        if (ManiacTypes.get(p) == ManiacTypes.ANGEL && SpectatorManager.deadPlayers(p.level().getServer()).isEmpty()) {
            p.displayClientMessage(Component.literal("Немає мертвих гравців для воскресіння."), true);
            return false;
        }
        return true;
    }

    public static void power2(ServerPlayer p) {
        if (ManiacTypes.get(p) == ManiacTypes.BEEKEEPER) {
            ServerLevel level = (ServerLevel) p.level();
            Vec3 at = p.getEyePosition().add(p.getLookAngle().scale(1.0));
            spawnBee(level, at, BEE_HP_POWER, false);
            spawnBee(level, at.add(0.4, 0.2, 0.0), BEE_HP_POWER, false);
            Sfx.at(p, SoundEvents.BEEHIVE_EXIT, 1.0F, 0.7F);
        } else {
            List<ServerPlayer> dead = SpectatorManager.deadPlayers(p.level().getServer());
            if (dead.isEmpty()) {
                p.displayClientMessage(Component.literal("Нікого воскрешати."), true);
                return;
            }
            revive(p, dead.get(p.getRandom().nextInt(dead.size())));
        }
    }

    private static void revive(ServerPlayer angel, ServerPlayer dead) {
        SpectatorManager.release(dead);
        dead.removeTag("manik_survivor");
        dead.addTag("manik_minion");
        dead.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        dead.getInventory().clearContent();
        dead.getInventory().setItem(0, new ItemStack(ManikItems.DAGGER.get()));
        dead.setHealth(dead.getMaxHealth());
        dead.getAbilities().invulnerable = true;
        dead.onUpdateAbilities();
        if (angel.level() instanceof ServerLevel lv) {
            dead.teleportTo(lv, angel.getX(), angel.getY(), angel.getZ(), Set.of(), dead.getYRot(), dead.getXRot(), true);
            lv.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, angel.getX(), angel.getY() + 1.0, angel.getZ(), 40, 0.5, 1.0, 0.5, 0.05);
        }
        StaminaManager.beginRound(dead);
        Sfx.at(dead, SoundEvents.WITHER_SPAWN, 0.6F, 1.5F);
        dead.sendSystemMessage(Component.literal("Тебе воскресив занепалий ангел. Тепер ти на боці маніяка: у тебе лише кинджал."));
        angel.displayClientMessage(Component.literal("Воскрешено: " + dead.getName().getString()), true);
    }

    /** Dagger attack of the resurrected minion. Always cancels vanilla damage. */
    public static void minionAttack(ServerPlayer attacker, Entity target) {
        ItemStack dagger = attacker.getMainHandItem();
        if (!dagger.is(ManikItems.DAGGER.get()) || StaminaManager.isDisabled(attacker)) return;
        if (!(target instanceof ServerPlayer t) || !isTarget(t)) return;
        if (attacker.getCooldowns().isOnCooldown(dagger)) {
            attacker.displayClientMessage(Component.literal("Кинджал перезаряджається."), true);
            return;
        }
        t.hurt(attacker.damageSources().generic(), DAGGER_DAMAGE);
        attacker.getCooldowns().addCooldown(dagger, DAGGER_COOLDOWN);
        Sfx.at(t, SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 1.0F);
        t.displayClientMessage(Component.literal("Кинджал: -4 HP."), true);
    }

    // ------------------------------------------------------- per-player tick
    private static final class Angel { double stamina = ANGEL_FLIGHT_TICKS; int lastPct = -1; boolean lastVisible; }
    private static final Map<UUID, Angel> ANGELS = new HashMap<>();

    private static void setFly(ServerPlayer p, boolean may, boolean flying) {
        var a = p.getAbilities();
        if (a.mayfly != may || a.flying != flying) {
            a.mayfly = may;
            a.flying = flying;
            p.onUpdateAbilities();
        }
    }

    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof ServerPlayer p)) return;
        UUID id = p.getUUID();

        // Beekeeper wings
        if (activeManiac(p, ManiacTypes.BEEKEEPER)) {
            if (flightLeft(p) > 0) {
                FLYING_BY_US.add(id);
                setFly(p, true, true);
                if (p.level() instanceof ServerLevel lv && p.tickCount % 2 == 0) {
                    double yaw = Math.toRadians(p.getYRot());
                    double bx = Math.sin(yaw) * 0.4, bz = -Math.cos(yaw) * 0.4;
                    double sx = Math.cos(yaw) * 0.5, sz = Math.sin(yaw) * 0.5;
                    for (int s = -1; s <= 1; s += 2) {
                        lv.sendParticles(ParticleTypes.END_ROD, p.getX() + bx + s * sx, p.getY() + 1.4, p.getZ() + bz + s * sz,
                                2, 0.15, 0.2, 0.15, 0.0);
                    }
                }
            } else if (FLYING_BY_US.remove(id)) {
                setFly(p, false, false);
            }
        } else if (FLYING_BY_US.remove(id)) {
            setFly(p, false, false);
        }

        // Fallen angel flight stamina
        if (activeManiac(p, ManiacTypes.ANGEL)) {
            Angel a = ANGELS.computeIfAbsent(id, k -> new Angel());
            boolean stunned = StaminaManager.isStunned(p);
            boolean flying = p.getAbilities().flying && !p.onGround();
            if (stunned) {
                setFly(p, false, false);
            } else {
                if (flying) {
                    a.stamina -= 1.0D;
                    if (a.stamina <= 0.0D) {
                        a.stamina = 0.0D;
                        setFly(p, false, false);
                        StaminaManager.stunKiller(p, ANGEL_STUN_TICKS);
                        p.displayClientMessage(Component.literal("Крила знесилили! Ти падаєш і оглушений на 5 с."), true);
                    }
                } else {
                    a.stamina = Math.min(ANGEL_FLIGHT_TICKS, a.stamina + ANGEL_REGEN_PER_TICK);
                }
                boolean may = a.stamina >= 5.0D || (flying && a.stamina > 0.0D);
                setFly(p, may, flying && may);
            }
            int pct = (int) Math.round(a.stamina / ANGEL_FLIGHT_TICKS * 100.0D);
            boolean visible = pct < 100 || flying;
            if (p.tickCount % 5 == 0 || pct != a.lastPct || visible != a.lastVisible) {
                ManikNetwork.sendStamina(p, pct, 0, 0, visible, true);
                a.lastPct = pct;
                a.lastVisible = visible;
            }
        } else if (ANGELS.remove(id) != null) {
            setFly(p, false, false);
            ManikNetwork.sendStamina(p, 100, 0, 0, false, true);
        }

        // Minion: invulnerable, hand-only dagger side
        if (isMinion(p) && GameConfig.isRunning() && !p.getAbilities().invulnerable) {
            p.getAbilities().invulnerable = true;
            p.onUpdateAbilities();
        }
    }

    public static void onServerTick(TickEvent.ServerTickEvent.Post event) {
        MinecraftServer server = event.server();
        serverTicks++;
        if (!GameConfig.isRunning()) return;
        tickPoison(server);
        tickBees(server);
        tickHives(server);
        tickCandles(server);
        TestMode.onServerTick(server);
    }

    /** Wipes every extra-maniac object and state (used by /test stop). */
    public static void clearAll(MinecraftServer server) {
        for (BeeState s : BEES.values()) if (!s.bee.isRemoved()) s.bee.discard();
        BEES.clear();
        for (Hive h : HIVES) {
            if (h.level().getBlockState(h.pos()).is(Blocks.BEE_NEST)) h.level().setBlock(h.pos(), Blocks.AIR.defaultBlockState(), 3);
        }
        HIVES.clear();
        for (Candle c : new ArrayList<>(CANDLES.values())) snuff(c, false);
        CANDLES.clear();
        BLOW.clear();
        POISON.clear();
        FLY_UNTIL.clear();
        ANGELS.clear();
        FLYING_BY_US.clear();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.getTags().contains("manik_minion")) {
                p.removeTag("manik_minion");
                p.getInventory().clearContent();
            }
            var a = p.getAbilities();
            if (!p.isCreative() && !p.isSpectator() && (a.mayfly || a.flying || a.invulnerable)) {
                a.mayfly = false;
                a.flying = false;
                a.invulnerable = false;
                p.onUpdateAbilities();
            }
        }
    }

    private ExtraManiacs() {}
}
