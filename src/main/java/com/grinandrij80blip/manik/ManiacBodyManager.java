package com.grinandrij80blip.manik;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server side of the custom maniac model. While a round is running, each maniac
 * is made invisible and a ManiacBodyEntity follows them around. Everything that
 * matters for gameplay still happens on the real player.
 */
public final class ManiacBodyManager {
    private static final Map<UUID, ManiacBodyEntity> BODIES = new HashMap<>();
    private static final Set<UUID> WAS_SWINGING = new HashSet<>();

    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        UUID id = player.getUUID();
        boolean maniac = GameConfig.isRunning()
                && player.isAlive()
                && player.getTags().contains("manik_killer");

        if (!maniac) {
            removeBody(player);
            return;
        }

        ManiacBodyEntity body = BODIES.get(id);
        if (body == null || body.isRemoved() || body.level() != level) {
            if (body != null) body.discard();
            body = new ManiacBodyEntity(ManikEntities.MANIAC_BODY.get(), level);
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

        // Mirror the player's arm swing so the model attacks together with the glove.
        boolean swinging = player.swinging;
        if (swinging && !WAS_SWINGING.contains(id)) {
            body.swing(InteractionHand.MAIN_HAND, true);
        }
        if (swinging) WAS_SWINGING.add(id); else WAS_SWINGING.remove(id);

        // The real player body is hidden; only the custom model is visible.
        if (player.tickCount % 10 == 0 || !player.hasEffect(MobEffects.INVISIBILITY)) {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false, false));
        }
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            removeBody(player);
        }
    }

    private static void removeBody(ServerPlayer player) {
        UUID id = player.getUUID();
        WAS_SWINGING.remove(id);
        ManiacBodyEntity body = BODIES.remove(id);
        if (body != null) {
            body.discard();
            player.removeEffect(MobEffects.INVISIBILITY);
        }
    }

    private ManiacBodyManager() {}
}
