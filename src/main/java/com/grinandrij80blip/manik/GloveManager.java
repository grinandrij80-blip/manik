package com.grinandrij80blip.manik;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.AttackEntityEvent;

/**
 * Handles the MANIK killer's glove on the logical server.
 * A valid hit deals one third of the survivor's maximum health and starts
 * a twelve-second item cooldown. Vanilla melee damage is cancelled for
 * that glove hit so it cannot be applied on top of the custom amount.
 */
public final class GloveManager {
    public static final int COOLDOWN_TICKS = 12 * 20;

    public static boolean onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer attacker)
                || !(event.getTarget() instanceof ServerPlayer target)) {
            return false;
        }

        ItemStack glove = attacker.getMainHandItem();
        if (!glove.is(ManikItems.MARK_GLOVE.get())
                || !GameConfig.isRunning()
                || !attacker.getTags().contains("manik_killer")
                || !target.getTags().contains("manik_survivor")) {
            return false;
        }

        // Cancel the ordinary hit; the glove supplies its own fixed damage.
        if (attacker.getCooldowns().isOnCooldown(glove)) {
            attacker.displayClientMessage(
                    Component.literal("Рукавиця перезаряджається."), true);
            return true;
        }

        float damage = target.getMaxHealth() / 3.0F;
        target.hurt(attacker.damageSources().playerAttack(attacker), damage);
        attacker.getCooldowns().addCooldown(glove, COOLDOWN_TICKS);

        attacker.displayClientMessage(
                Component.literal("Рукавицю використано. Перезарядка: 12 секунд."), true);
        target.displayClientMessage(
                Component.literal("Тебе влучили рукавицею! Втрачено 1/3 максимального здоров'я."), true);
        return true;
    }

    private GloveManager() {}
}
