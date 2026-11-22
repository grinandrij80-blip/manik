package com.grinandrij80blip.manik;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Macadamia nut: eaten instantly. 50% heals 4 hearts, 50% poisons for 4 hearts of damage
 * (Poison I for 10 s: one heart-half every 1.25 s, eight times).
 */
public final class MacadamiaItem extends Item {
    public static final float HEAL_AMOUNT = 8.0F;
    public static final int POISON_TICKS = 200;

    public MacadamiaItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        ItemStack stack = player.getItemInHand(hand);
        Sfx.at(player, SoundEvents.GENERIC_EAT, 0.8F, 1.0F);
        if (player.getRandom().nextBoolean()) {
            player.heal(HEAL_AMOUNT);
            player.displayClientMessage(Component.literal("Горіх виявився корисним: +4 серця."), true);
        } else {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_TICKS, 0, false, true, true));
            player.displayClientMessage(Component.literal("Горіх був отруйним! Отрута забере 4 серця."), true);
        }
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResult.SUCCESS;
    }
}
