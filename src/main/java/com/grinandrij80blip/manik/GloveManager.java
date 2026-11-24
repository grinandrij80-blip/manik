package com.grinandrij80blip.manik;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
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
    /** Fixed glove damage in health points (8 HP = 4 hearts). */
    public static final float HIT_DAMAGE = 8.0F;
    /** How long a crowbar hit stuns the maniac. */
    public static final int CROWBAR_STUN_TICKS = 3 * 20;
    /** The stun gun stuns the maniac longer and never slips. */
    public static final int STUN_GUN_STUN_TICKS = 8 * 20;
    /** Chance that a crowbar hit slips: durability spent, no stun. */
    public static final float CROWBAR_SLIP_CHANCE = 0.20F;
    /** After a slip the crowbar cooldown is this much shorter. */
    public static final int SLIP_COOLDOWN_REDUCTION_TICKS = 5 * 20;

    public static boolean onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer attacker) || !GameConfig.isRunning()) {
            return false;
        }

        boolean killer = attacker.getTags().contains("manik_killer");
        boolean survivor = attacker.getTags().contains("manik_survivor");

        // A maniac cannot use an empty hand or any other item to attack.
        if (killer) {
            if (StaminaManager.isDisabled(attacker)) {
                attacker.displayClientMessage(
                        Component.literal("Ти оглушений і не можеш атакувати."), true);
                return true;
            }

            ItemStack glove = attacker.getMainHandItem();
            if (!glove.is(ManikItems.MARK_GLOVE.get())) {
                attacker.displayClientMessage(
                        Component.literal("Маніяк може атакувати лише ножем."), true);
                return true;
            }

            if (!(event.getTarget() instanceof ServerPlayer target)
                    || !target.getTags().contains("manik_survivor")) {
                return true;
            }

            // Keep the glove hit from applying vanilla melee damage as well.
            if (attacker.getCooldowns().isOnCooldown(glove)) {
                attacker.displayClientMessage(
                        Component.literal("Ніж перезаряджається."), true);
                return true;
            }

            // Every successful glove hit deals fixed damage; there is no guaranteed kill.
            target.hurt(attacker.damageSources().generic(), HIT_DAMAGE);

            Sfx.at(target, net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 0.8F);
            attacker.getCooldowns().addCooldown(glove, COOLDOWN_TICKS);
            attacker.displayClientMessage(
                    Component.literal("Ніж використано. Перезарядка: 12 секунд."), true);
            target.displayClientMessage(
                    Component.literal("Влучання ножем: -8 HP."), true);
            return true;
        }

        // Survivors can hit the maniac only with the crowbar or the stun gun, which
        // stun instead of damaging. Every other attack on the maniac is blocked.
        if (survivor && event.getTarget() instanceof ServerPlayer target
                && target.getTags().contains("manik_killer")) {
            ItemStack weapon = attacker.getMainHandItem();
            boolean crowbar = weapon.is(ManikItems.CROWBAR.get());
            boolean stunGun = weapon.is(ManikItems.STUN_GUN.get());
            if (!crowbar && !stunGun) {
                attacker.displayClientMessage(
                        Component.literal("Маніяка можна вдарити лише ломом або електрошокером."), true);
                return true;
            }

            if (attacker.getCooldowns().isOnCooldown(weapon)) {
                attacker.displayClientMessage(
                        Component.literal(crowbar ? "Лом перезаряджається." : "Електрошокер перезаряджається."), true);
                return true;
            }

            if (StaminaManager.isStunned(target)) {
                attacker.displayClientMessage(
                        Component.literal("Маніяк уже оглушений."), true);
                return true;
            }

            // The crowbar can slip: durability is spent, nobody is stunned, and the
            // cooldown is 5 seconds shorter. The stun gun never slips.
            if (crowbar && attacker.getRandom().nextFloat() < CROWBAR_SLIP_CHANCE) {
                Sfx.at(target, net.minecraft.sounds.SoundEvents.SHIELD_BLOCK, 1.0F, 1.2F);
                attacker.getCooldowns().addCooldown(weapon,
                        Math.max(1, TrapManager.CROWBAR_COOLDOWN_TICKS - SLIP_COOLDOWN_REDUCTION_TICKS));
                CrowbarItem.consumeUse(attacker, InteractionHand.MAIN_HAND, weapon);
                attacker.connection.send(new ClientboundSetTitlesAnimationPacket(0, 20, 0));
                attacker.connection.send(new ClientboundSetTitleTextPacket(
                        Component.literal("Зіскочило!").withStyle(style -> style.withColor(0xFF8C1A))));
                return true;
            }

            int stunTicks = crowbar ? CROWBAR_STUN_TICKS : STUN_GUN_STUN_TICKS;
            StaminaManager.stunKiller(target, stunTicks);
            Sfx.maniacHurt(target);
            if (stunGun) {
                Sfx.at(target, net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_IMPACT, 0.6F, 1.8F);
            }

            if (stunGun) {
                ShockManager.start(target, stunTicks);
            }

            if (crowbar) {
                // Knock the stunned maniac away from the survivor.
                target.knockback(1.6D, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
                target.hurtMarked = true;
            }

            attacker.getCooldowns().addCooldown(weapon, TrapManager.CROWBAR_COOLDOWN_TICKS);
            CrowbarItem.consumeUse(attacker, InteractionHand.MAIN_HAND, weapon);

            int seconds = stunTicks / 20;
            attacker.displayClientMessage(
                    Component.literal("Маніяка оглушено на " + seconds + " с! Предмет втратив одне використання."), true);
            target.displayClientMessage(
                    Component.literal("Тебе оглушили! Рух, стрибок і атаки заблоковані на " + seconds + " с."), true);
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
