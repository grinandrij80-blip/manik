package com.grinandrij80blip.manik;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Visual stand-in for a clown maniac; behaves exactly like ManiacBodyEntity. */
public final class ClownBodyEntity extends ManiacBodyEntity {
    public ClownBodyEntity(EntityType<? extends ManiacBodyEntity> type, Level level) {
        super(type, level);
    }
}
