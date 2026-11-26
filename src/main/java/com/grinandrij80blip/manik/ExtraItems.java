package com.grinandrij80blip.manik;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Items of the test-only maniacs. They are not in the creative tab and not in round loot. */
public final class ExtraItems {

    public static final class FeatherBow extends Item {
        public FeatherBow(Properties properties) { super(properties); }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                ExtraManiacs.shootBow(sp, player.getItemInHand(hand));
            }
            return InteractionResult.SUCCESS;
        }
    }

    public static final class MiniHive extends Item {
        public MiniHive(Properties properties) { super(properties); }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            if (!context.getLevel().isClientSide() && context.getPlayer() instanceof ServerPlayer sp) {
                ExtraManiacs.placeHive(sp, context.getClickedPos().relative(context.getClickedFace()), context.getItemInHand());
            }
            return InteractionResult.SUCCESS;
        }
    }

    public static final class CandleItem extends Item {
        public CandleItem(Properties properties) { super(properties); }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            if (!context.getLevel().isClientSide() && context.getPlayer() instanceof ServerPlayer sp) {
                ExtraManiacs.placeCandle(sp, context.getClickedPos().relative(context.getClickedFace()), context.getItemInHand());
            }
            return InteractionResult.SUCCESS;
        }
    }

    public static final class Honey extends Item {
        public Honey(Properties properties) { super(properties); }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                ExtraManiacs.drinkHoney(sp, player.getItemInHand(hand));
            }
            return InteractionResult.SUCCESS;
        }
    }

    private ExtraItems() {}
}
