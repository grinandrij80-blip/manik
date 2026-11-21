package com.grinandrij80blip.manik;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Visual stand-in for a maniac. It has no physics, cannot be hit or pushed and is
 * never saved; ManiacBodyManager moves it onto the maniac every tick on the server.
 * Walking is derived from how far the entity moved, so it works the same on the
 * client without extra packets. The attack swing uses the vanilla swing animation.
 */
public final class ManiacBodyEntity extends LivingEntity {
    public float animPos;
    public float animPosO;
    public float animSpeed;
    public float animSpeedO;

    public ManiacBodyEntity(EntityType<? extends ManiacBodyEntity> type, Level level) {
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
        // Attacks and ray traces must reach the real player, not this stand-in.
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void tick() {
        this.animPosO = this.animPos;
        this.animSpeedO = this.animSpeed;
        super.tick();

        double dx = this.getX() - this.xo;
        double dz = this.getZ() - this.zo;
        float moved = (float) Math.sqrt(dx * dx + dz * dz);
        float target = Math.min(1.0F, moved * 4.0F);
        this.animSpeed += (target - this.animSpeed) * 0.4F;
        this.animPos += this.animSpeed * 0.8F;
    }
}
