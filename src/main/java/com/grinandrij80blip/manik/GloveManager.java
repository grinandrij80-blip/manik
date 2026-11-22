package com.grinandrij80blip.manik;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.AttackEntityEvent;

/**
 * Server-authoritative MANIK role attack restrictions and glove hits.
 * A survivor holding the crowbar can hit the maniac to stun them for a few
 * seconds; the crowbar loses one use and deals no damage.
 */
public final class GloveManager {
    public static final int COOLDOWN_TICKS = 12 * 20;
    /** Fixed glove damage in health points (4 HP = 2 hearts). */
    public static final float HIT_DAMAGE = 4.0F;
    /** How long a crowbar hit stuns the maniac. */
    public static final int CROWBAR_STUN_TICKS = 3 * 20;

    public static boolean onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer attacker) || !GameConfig.isRunning()) {
            return false;
        }

        boolean killer = attacker.getTags().contains("manik_killer");
        boolean survivor = attacker.getTags().contains("manik_survivor");

        // A maniac cannot use an empty hand or any other item to attack.
        if (killer) {
            if (StaminaManager.isStunned(attacker)) {
                attacker.displayClientMessage(
                        Component.literal("Ти оглушений і не можеш атакувати."), true);
                return true;
            }

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

            // Every successful glove hit deals fixed damage; there is no guaranteed kill.
            target.hurt(attacker.damageSources().generic(), HIT_DAMAGE);

            attacker.getCooldowns().addCooldown(glove, COOLDOWN_TICKS);
            attacker.displayClientMessage(
                    Component.literal("Рукавицю використано. Перезарядка: 12 секунд."), true);
            target.displayClientMessage(
                    Component.literal("Влучання рукавицею: -4 HP."), true);
            return true;
        }

        // Survivors can hit the maniac only with the crowbar, which stuns
        // instead of damaging. Every other attack on the maniac is blocked.
        if (survivor && event.getTarget() instanceof ServerPlayer target
                && target.getTags().contains("manik_killer")) {
            ItemStack crowbar = attacker.getMainHandItem();
            if (!crowbar.is(ManikItems.CROWBAR.get())) {
                attacker.displayClientMessage(
                        Component.literal("Маніяка можна вдарити лише ломом."), true);
                return true;
            }

            if (attacker.getCooldowns().isOnCooldown(crowbar)) {
                attacker.displayClientMessage(
                        Component.literal("Лом перезаряджається."), true);
                return true;
            }

            if (!StaminaManager.stunKiller(target, CROWBAR_STUN_TICKS)) {
                attacker.displayClientMessage(
                        Component.literal("Маніяк уже оглушений."), true);
                return true;
            }

            // Knock the stunned maniac away from the survivor.
            target.knockback(1.6D, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
            target.hurtMarked = true;

            attacker.getCooldowns().addCooldown(crowbar, TrapManager.CROWBAR_COOLDOWN_TICKS);
            CrowbarItem.consumeUse(attacker, InteractionHand.MAIN_HAND, crowbar);

            attacker.displayClientMessage(
                    Component.literal("Маніяка оглушено на 3 секунди! Лом втратив одне використання."), true);
            target.displayClientMessage(
                    Component.literal("Тебе оглушили ломом! Рух, стрибок і атаки заблоковані на 3 секунди."), true);
            return true;
        }

        return false;
    }

    public static void resetRound() {
    }

    public static void onLogout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
    }

    private GloveManager() {}
}
