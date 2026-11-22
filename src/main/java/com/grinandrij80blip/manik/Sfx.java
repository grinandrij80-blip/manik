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

    public static void at(Level level, double x, double y, double z,
                          net.minecraft.core.Holder<SoundEvent> sound, float volume, float pitch) {
        at(level, x, y, z, sound.value(), volume, pitch);
    }

    public static void at(Entity entity, net.minecraft.core.Holder<SoundEvent> sound, float volume, float pitch) {
        at(entity, sound.value(), volume, pitch);
    }

    /** Plays a sound only for one player, at their own position. */
    public static void toPlayer(net.minecraft.server.level.ServerPlayer player,
                                net.minecraft.core.Holder<SoundEvent> sound, float volume, float pitch) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                sound, SoundSource.HOSTILE, player.getX(), player.getY(), player.getZ(),
                volume, pitch, player.getRandom().nextLong()));
    }

    public static void toPlayer(net.minecraft.server.level.ServerPlayer player,
                                SoundEvent sound, float volume, float pitch) {
        toPlayer(player, net.minecraft.core.Holder.direct(sound), volume, pitch);
    }

    /** Short, high-pitched pained groan of the maniac when he is hit. */
    public static void maniacHurt(Entity maniac) {
        at(maniac, net.minecraft.sounds.SoundEvents.ZOMBIE_HURT, 1.0F, 1.7F);
    }

    private Sfx() {}
}
