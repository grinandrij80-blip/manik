package com.grinandrij80blip.manik;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Persistent world settings and transient session state for MANIK.
 * Generator and killer counts are stored in the Overworld SavedData.
 */
public final class GameConfig {
    private static int generators = 3;
    private static int killers = 1;
    private static boolean running = false;

    private static ServerLevel loadedWorld;
    private static SettingsData savedSettings;

    private static final Codec<SettingsData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.optionalFieldOf("generators", 3)
                            .forGetter(data -> data.generators),
                    Codec.INT.optionalFieldOf("killers", 1)
                            .forGetter(data -> data.killers)
            ).apply(instance, SettingsData::new)
    );

    private static final SavedDataType<SettingsData> DATA_TYPE =
            new SavedDataType<>("manik_game_settings", SettingsData::new, CODEC, null);

    private static final class SettingsData extends SavedData {
        private int generators;
        private int killers;

        private SettingsData() {
            this(3, 1);
        }

        private SettingsData(int generators, int killers) {
            this.generators = generators;
            this.killers = killers;
        }
    }

    /**
     * Loads saved settings from this server's Overworld.
     * Call before reading or changing the configuration from a command.
     */
    public static void load(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        if (loadedWorld == overworld && savedSettings != null) return;

        loadedWorld = overworld;
        savedSettings = overworld.getDataStorage().computeIfAbsent(DATA_TYPE);

        int normalizedGenerators = Math.max(1, Math.min(100, savedSettings.generators));
        int normalizedKillers = Math.max(1, Math.min(100, savedSettings.killers));
        if (normalizedGenerators != savedSettings.generators
                || normalizedKillers != savedSettings.killers) {
            savedSettings.generators = normalizedGenerators;
            savedSettings.killers = normalizedKillers;
            savedSettings.setDirty();
        }

        generators = normalizedGenerators;
        killers = normalizedKillers;
    }

    public static int generators() {
        return generators;
    }

    public static void setGenerators(int value) {
        generators = Math.max(1, Math.min(100, value));
        if (savedSettings != null) {
            savedSettings.generators = generators;
            savedSettings.setDirty();
        }
    }

    public static int killers() {
        return killers;
    }

    public static void setKillers(int value) {
        killers = Math.max(1, Math.min(100, value));
        if (savedSettings != null) {
            savedSettings.killers = killers;
            savedSettings.setDirty();
        }
    }

    public static boolean isRunning() {
        return running;
    }

    public static void setRunning(boolean value) {
        running = value;
    }

    private GameConfig() {
    }
}
