package com.grinandrij80blip.manik;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Tracks MANIK spawn markers in per-dimension SavedData.
 *
 * Existing marker blocks are imported when their chunks load, so worlds made
 * before this persistence feature do not need their spawn markers replaced.
 */
public final class SpawnPointManager {
    public enum Type {
        ITEM,
        GENERATOR,
        SURVIVOR,
        KILLER,
        EXIT
    }

    private static final String DATA_ID = "manik_spawn_points";
    private static final Codec<List<Long>> POSITIONS_CODEC = Codec.LONG.listOf();

    private static final Codec<SpawnPointData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    POSITIONS_CODEC.optionalFieldOf("item", List.of())
                            .forGetter(data -> data.toLongList(Type.ITEM)),
                    POSITIONS_CODEC.optionalFieldOf("generator", List.of())
                            .forGetter(data -> data.toLongList(Type.GENERATOR)),
                    POSITIONS_CODEC.optionalFieldOf("survivor", List.of())
                            .forGetter(data -> data.toLongList(Type.SURVIVOR)),
                    POSITIONS_CODEC.optionalFieldOf("killer", List.of())
                            .forGetter(data -> data.toLongList(Type.KILLER)),
                    POSITIONS_CODEC.optionalFieldOf("exit", List.of())
                            .forGetter(data -> data.toLongList(Type.EXIT))
            ).apply(instance, SpawnPointData::new)
    );

    private static final SavedDataType<SpawnPointData> DATA_TYPE =
            new SavedDataType<>(DATA_ID, SpawnPointData::new, CODEC, null);

    private static final class SpawnPointData extends SavedData {
        private final EnumMap<Type, Set<BlockPos>> points = new EnumMap<>(Type.class);

        private SpawnPointData() {
            for (Type type : Type.values()) {
                points.put(type, new HashSet<>());
            }
        }

        private SpawnPointData(
                List<Long> item,
                List<Long> generator,
                List<Long> survivor,
                List<Long> killer,
                List<Long> exit
        ) {
            this();
            loadPositions(Type.ITEM, item);
            loadPositions(Type.GENERATOR, generator);
            loadPositions(Type.SURVIVOR, survivor);
            loadPositions(Type.KILLER, killer);
            loadPositions(Type.EXIT, exit);
        }

        private void loadPositions(Type type, List<Long> packedPositions) {
            Set<BlockPos> destination = points.get(type);
            for (long packedPosition : packedPositions) {
                destination.add(BlockPos.of(packedPosition));
            }
        }

        private List<Long> toLongList(Type type) {
            List<Long> packedPositions = new ArrayList<>(points.get(type).size());
            for (BlockPos pos : points.get(type)) {
                packedPositions.add(pos.asLong());
            }
            return packedPositions;
        }

        private boolean add(Type type, BlockPos pos) {
            boolean changed = points.get(type).add(pos.immutable());
            if (changed) {
                setDirty();
            }
            return changed;
        }

        private boolean remove(Type type, BlockPos pos) {
            boolean changed = points.get(type).remove(pos);
            if (changed) {
                setDirty();
            }
            return changed;
        }

        private List<BlockPos> get(Type type) {
            return new ArrayList<>(points.get(type));
        }
    }

    private static SpawnPointData data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(DATA_TYPE);
    }

    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Type type = typeOf(event.getPlacedBlock().getBlock());
        if (type != null) {
            data(level).add(type, event.getPos());
        }
    }

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Type type = typeOf(event.getState().getBlock());
        if (type != null) {
            data(level).remove(type, event.getPos());
        }
    }

    /**
     * Imports marker blocks that were already present in a world before this
     * SavedData implementation existed. Scanning the ChunkAccess directly
     * avoids requesting chunk loads while the chunk-load event is running.
     */
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        ChunkAccess chunk = event.getChunk();
        ChunkPos chunkPos = chunk.getPos();
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        SpawnPointData savedPoints = data(level);

        for (int y = level.getMinY(); y < level.getMaxY(); y++) {
            for (int x = minX; x < minX + 16; x++) {
                for (int z = minZ; z < minZ + 16; z++) {
                    cursor.set(x, y, z);
                    Type type = typeOf(chunk.getBlockState(cursor).getBlock());
                    if (type != null) {
                        savedPoints.add(type, cursor);
                    }
                }
            }
        }
    }

    public static List<BlockPos> get(ServerLevel level, Type type) {
        return data(level).get(type);
    }

    private static Type typeOf(Block block) {
        if (block == ManikBlocks.ITEM_SPAWN.get()) {
            return Type.ITEM;
        }
        if (block == ManikBlocks.GENERATOR_SPAWN.get()) {
            return Type.GENERATOR;
        }
        if (block == ManikBlocks.SURVIVOR_SPAWN.get()) {
            return Type.SURVIVOR;
        }
        if (block == ManikBlocks.KILLER_SPAWN.get()) {
            return Type.KILLER;
        }
        if (block == ManikBlocks.EXIT_SPAWN.get()) {
            return Type.EXIT;
        }
        return null;
    }

    private SpawnPointManager() {
    }
}
