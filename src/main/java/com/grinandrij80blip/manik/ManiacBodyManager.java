package com.grinandrij80blip.manik;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server side of the custom maniac model. While a round is running, each maniac
 * is made invisible and a ManiacBodyEntity follows them around. Everything that
 * matters for gameplay still happens on the real player.
 */
public final class ManiacBodyManager {
    private static final Map<UUID, ManiacBodyEntity> BODIES = new HashMap<>();

    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        UUID id = player.getUUID();
        boolean maniac = GameConfig.isRunning()
                && player.isAlive()
                && player.getTags().contains("manik_killer");

        if (!maniac) {
            removeBody(player);
            ClownManager.remove(id);
            return;
        }

        int mtype = ManiacTypes.get(player);
        ManiacBodyEntity body = BODIES.get(id);
        if (body == null || body.isRemoved() || body.level() != level || bodyType(body) != mtype) {
            if (body != null) body.discard();
            body = switch (mtype) {
                case ManiacTypes.CLOWN -> new ClownBodyEntity(ManikEntities.CLOWN_BODY.get(), level);
                case ManiacTypes.BEEKEEPER -> new BeekeeperBodyEntity(ManikEntities.BEEKEEPER_BODY.get(), level);
                case ManiacTypes.ANGEL -> new AngelBodyEntity(ManikEntities.ANGEL_BODY.get(), level);
                default -> new ManiacBodyEntity(ManikEntities.MANIAC_BODY.get(), level);
            };
            body.setOwnerId(player.getId());
            body.setPos(player.getX(), player.getY(), player.getZ());
            body.setYRot(player.getYRot());
            body.setXRot(player.getXRot());
            body.setYHeadRot(player.getYHeadRot());
            level.addFreshEntity(body);
            BODIES.put(id, body);
        }

        body.setPos(player.getX(), player.getY(), player.getZ());
        body.setYRot(player.getYRot());
        body.setXRot(player.getXRot());
        body.setYHeadRot(player.getYHeadRot());
        body.setWings(ExtraManiacs.wingsOut(player));
        // An invisible clown hides the model, too.
        boolean hidden = ClownManager.isInvisible(player);
        if (body.isInvisible() != hidden) body.setInvisible(hidden);
        ClownManager.onPlayerTick(player);

        // The real player body is hidden; only the custom model is visible.
        if (player.tickCount % 10 == 0 || !player.hasEffect(MobEffects.INVISIBILITY)) {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false, false));
        }
    }

    private static int bodyType(ManiacBodyEntity body) {
        if (body instanceof ClownBodyEntity) return ManiacTypes.CLOWN;
        if (body instanceof BeekeeperBodyEntity) return ManiacTypes.BEEKEEPER;
        if (body instanceof AngelBodyEntity) return ManiacTypes.ANGEL;
        return ManiacTypes.BRUTE;
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            removeBody(player);
        }
    }

    public static void removeBody(ServerPlayer player) {
        ManiacBodyEntity body = BODIES.remove(player.getUUID());
        if (body != null) {
            body.discard();
            player.removeEffect(MobEffects.INVISIBILITY);
        }
    }

    private ManiacBodyManager() {}
}
