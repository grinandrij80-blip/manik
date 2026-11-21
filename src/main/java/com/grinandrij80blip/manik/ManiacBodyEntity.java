package com.grinandrij80blip.manik;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Visual stand-in for a maniac. It has no physics, cannot be hit or pushed and is
 * never saved. The server spawns it and tells every client which player it belongs
 * to; clients then copy that player's position, rotation, walk cycle and arm swing
 * every frame (see ManiacBodyRenderer), so the model moves exactly with the maniac
 * instead of trailing behind network updates.
 */
public final class ManiacBodyEntity extends LivingEntity {
    private static final EntityDataAccessor<Integer> OWNER_ID =
            SynchedEntityData.defineId(ManiacBodyEntity.class, EntityDataSerializers.INT);

    // Fallback walk animation, used only while the owner is not known on the client.
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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER_ID, -1);
    }

    /** Entity id of the maniac player this body belongs to, or -1. */
    public int getOwnerId() {
        return this.entityData.get(OWNER_ID);
    }

    public void setOwnerId(int ownerId) {
        this.entityData.set(OWNER_ID, ownerId);
    }

    /** The owning player as known on this side of the connection, or null. */
    public Entity getOwner() {
        int id = this.getOwnerId();
        return id < 0 ? null : this.level().getEntity(id);
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
        if (this.level().isClientSide()) {
            Entity owner = this.getOwner();
            if (owner != null) {
                // Keep the bounding box (and so frustum culling) on the player.
                this.setPos(owner.getX(), owner.getY(), owner.getZ());
            }
        }

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
