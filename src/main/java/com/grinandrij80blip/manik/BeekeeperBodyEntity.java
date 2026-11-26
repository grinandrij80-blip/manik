package com.grinandrij80blip.manik;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Visual stand-in for the Beekeeper maniac. */
public final class BeekeeperBodyEntity extends ManiacBodyEntity {
    public BeekeeperBodyEntity(EntityType<? extends ManiacBodyEntity> type, Level level) {
        super(type, level);
    }
}
