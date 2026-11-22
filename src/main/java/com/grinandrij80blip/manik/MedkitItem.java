package com.grinandrij80blip.manik;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

public final class MedkitItem extends Item {
    public static final int USE_TICKS = 60;
    /** Fixed healing in health points (8 HP = 4 hearts). */
    public static final float HEAL_AMOUNT = 8.0F;

    public MedkitItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_TICKS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingUseDuration) {
        if (!level.isClientSide() && level.getGameTime() % 5L == 0L) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 8, 0, true, false, false));
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity living) {
        if (!level.isClientSide() && living instanceof Player player) {
            player.heal(HEAL_AMOUNT);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return stack;
    }
}
