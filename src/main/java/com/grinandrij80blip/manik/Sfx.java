package com.grinandrij80blip.manik;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/** Small helper for world sounds played from the server (everyone nearby hears them). */
public final class Sfx {
    public static void at(Level level, double x, double y, double z, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, x, y, z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    public static void at(Entity entity, SoundEvent sound, float volume, float pitch) {
        at(entity.level(), entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                sound, volume, pitch);
    }

    /** Short, high-pitched pained groan of the maniac when he is hit. */
    public static void maniacHurt(Entity maniac) {
        at(maniac, net.minecraft.sounds.SoundEvents.ZOMBIE_HURT, 1.0F, 1.7F);
    }

    private Sfx() {}
}
