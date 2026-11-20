package com.grinandrij80blip.manik;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.AttackEntityEvent;

/**
 * Server-authoritative hit handling for the MANIK glove.
 * The glove only works for the killer against a survivor during an active round.
 */
public final class GloveManager {
    public static final int COOLDOWN_TICKS = 12 * 20;

    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer attacker)
                || !(event.getTarget() instanceof ServerPlayer target)
                || !attacker.getMainHandItem().is(ManikItems.MARK_GLOVE.get())) {
            return;
        }

        // Replace vanilla melee damage with the glove's single, fixed game effect.
        event.setCanceled(true);

        if (!GameConfig.isRunning()
                || !attacker.getTags().contains("manik_killer")
                || !target.getTags().contains("manik_survivor")) {
            attacker.displayClientMessage(
                    Component.literal("Рукавиця працює лише під час раунду: маніяк проти вижившого."),
                    true);
            return;
        }

        ItemStack glove = attacker.getMainHandItem();
        if (attacker.getCooldowns().isOnCooldown(glove)) {
            attacker.displayClientMessage(
                    Component.literal("Рукавиця ще перезаряджається."),
                    true);
            return;
        }

        float damage = target.getMaxHealth() / 3.0F;
        target.hurt(attacker.level().damageSources().generic(), damage);
        attacker.getCooldowns().addCooldown(glove, COOLDOWN_TICKS);

        attacker.displayClientMessage(
                Component.literal("Рукавицю використано. Перезарядка: 12 секунд."),
                true);
        target.displayClientMessage(
                Component.literal("Тебе влучили рукавицею! Втрачено 1/3 максимального здоров'я."),
                true);
    }

    private GloveManager() {}
}
