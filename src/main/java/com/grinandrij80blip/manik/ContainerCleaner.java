package com.grinandrij80blip.manik;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Empties every chest, barrel, shulker box, hopper, furnace and other container
 * near the map (loaded chunks within 6 chunks of any spawn marker), at the start and
 * at the end of a round.
 */
public final class ContainerCleaner {
    private static final int CHUNK_RADIUS = 6;

    public static int clear(ServerLevel level) {
        if (level == null) return 0;
        Set<Long> chunks = new HashSet<>();
        for (SpawnPointManager.Type type : SpawnPointManager.Type.values()) {
            for (BlockPos pos : SpawnPointManager.get(level, type)) {
                int cx = pos.getX() >> 4;
                int cz = pos.getZ() >> 4;
                for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
                    for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
                        chunks.add(ChunkPos.asLong(cx + dx, cz + dz));
                    }
                }
            }
        }
        int cleared = 0;
        for (long key : chunks) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(ChunkPos.getX(key), ChunkPos.getZ(key));
            if (chunk == null) continue;
            for (BlockEntity entity : new ArrayList<>(chunk.getBlockEntities().values())) {
                if (entity instanceof Container container && !container.isEmpty()) {
                    container.clearContent();
                    entity.setChanged();
                    cleared++;
                }
            }
        }
        return cleared;
    }

    private ContainerCleaner() {}
}
