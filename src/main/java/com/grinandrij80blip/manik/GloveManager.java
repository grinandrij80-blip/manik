package com.grinandrij80blip.manik;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.AttackEntityEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-authoritative MANIK role attack restrictions and glove hits.
 * The crowbar remains a trap-disarming utility item, not a player-attack item.
 */
public final class GloveManager {
    public static final int COOLDOWN_TICKS = 12 * 20;
    private static final Map<UUID, Integer> GLOVE_HITS_RECEIVED = new HashMap<>();

    public static boolean onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer attacker) || !GameConfig.isRunning()) {
            return false;
        }

        boolean killer = attacker.getTags().contains("manik_killer");
        boolean survivor = attacker.getTags().contains("manik_survivor");

        // A maniac cannot use an empty hand or any other item to attack.
        if (killer) {
            ItemStack glove = attacker.getMainHandItem();
            if (!glove.is(ManikItems.MARK_GLOVE.get())) {
                attacker.displayClientMessage(
                        Component.literal("Маніяк може атакувати лише рукавицею."), true);
                return true;
            }

            if (!(event.getTarget() instanceof ServerPlayer target)
                    || !target.getTags().contains("manik_survivor")) {
                return true;
            }

            // Keep the glove hit from applying vanilla melee damage as well.
            if (attacker.getCooldowns().isOnCooldown(glove)) {
                attacker.displayClientMessage(
                        Component.literal("Рукавиця перезаряджається."), true);
                return true;
            }

            int hits = GLOVE_HITS_RECEIVED.getOrDefault(target.getUUID(), 0) + 1;
            GLOVE_HITS_RECEIVED.put(target.getUUID(), hits);

            if (hits >= 3) {
                // The third successful glove hit eliminates the survivor even
                // if armor or other mitigation reduced the first two hits.
                target.hurt(attacker.damageSources().genericKill(), Float.MAX_VALUE);
            } else {
                float damage = target.getMaxHealth() / 3.0F;
                target.hurt(attacker.damageSources().generic(), damage);
            }

            attacker.getCooldowns().addCooldown(glove, COOLDOWN_TICKS);
            attacker.displayClientMessage(
                    Component.literal("Рукавицю використано. Удари по цілі: "
                            + Math.min(hits, 3) + "/3. Перезарядка: 12 секунд."), true);

            if (hits >= 3) {
                GLOVE_HITS_RECEIVED.remove(target.getUUID());
                target.displayClientMessage(
                        Component.literal("Ти вибув із раунду після третього влучання рукавицею."), true);
            } else {
                target.displayClientMessage(
                        Component.literal("Влучання рукавицею: " + hits
                                + "/3. Третє влучання вибуває з раунду."), true);
            }
            return true;
        }

        // Survivors cannot punch the maniac; the crowbar remains for traps.
        if (survivor && event.getTarget() instanceof ServerPlayer target
                && target.getTags().contains("manik_killer")) {
            attacker.displayClientMessage(
                    Component.literal("Голими руками маніяка не вдарити. Лом призначений для капканів."), true);
            return true;
        }

        return false;
    }

    public static void resetRound() {
        GLOVE_HITS_RECEIVED.clear();
    }

    public static void onLogout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        GLOVE_HITS_RECEIVED.remove(event.getEntity().getUUID());
    }

    private GloveManager() {}
}
