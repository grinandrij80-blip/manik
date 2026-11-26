package com.grinandrij80blip.manik;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Visual stand-in for the Fallen Angel maniac. */
public final class AngelBodyEntity extends ManiacBodyEntity {
    public AngelBodyEntity(EntityType<? extends ManiacBodyEntity> type, Level level) {
        super(type, level);
    }
}
