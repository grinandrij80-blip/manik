package com.grinandrij80blip.manik;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** Rescue helicopter: a decoration moved by ExitManager. No physics, cannot be hit, never saved. */
public final class HelicopterEntity extends LivingEntity {
    public HelicopterEntity(EntityType<? extends HelicopterEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
