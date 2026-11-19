package com.grinandrij80blip.manik;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.level.BlockEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SpawnPointManager {
    public enum Type { ITEM, GENERATOR, SURVIVOR, KILLER }
    private static final Map<ResourceKey<Level>, EnumMap<Type, List<BlockPos>>> POINTS = new HashMap<>();

    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Type type = typeOf(event.getPlacedBlock().getBlock());
        if (type != null) points(level).get(type).add(event.getPos().immutable());
    }

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Type type = typeOf(event.getState().getBlock());
        if (type != null) points(level).get(type).remove(event.getPos());
    }

    public static List<BlockPos> get(ServerLevel level, Type type) {
        return new ArrayList<>(points(level).get(type));
    }

    private static EnumMap<Type, List<BlockPos>> points(ServerLevel level) {
        return POINTS.computeIfAbsent(level.dimension(), key -> {
            EnumMap<Type, List<BlockPos>> map = new EnumMap<>(Type.class);
            for (Type type : Type.values()) map.put(type, new ArrayList<>());
            return map;
        });
    }

    private static Type typeOf(Block block) {
        if (block == ManikBlocks.ITEM_SPAWN.get()) return Type.ITEM;
        if (block == ManikBlocks.GENERATOR_SPAWN.get()) return Type.GENERATOR;
        if (block == ManikBlocks.SURVIVOR_SPAWN.get()) return Type.SURVIVOR;
        if (block == ManikBlocks.KILLER_SPAWN.get()) return Type.KILLER;
        return null;
    }
    private SpawnPointManager() {}
}