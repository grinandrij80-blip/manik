package com.grinandrij80blip.manik;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** End-of-round cleanup: empty inventories, no items on the ground, healed players sent to the lobby. */
public final class RoundCleanup {
    public static void finish(MinecraftServer server, ServerLevel level) {
        // Every item entity in every loaded dimension disappears.
        for (ServerLevel l : server.getAllLevels()) {
            List<Entity> items = new ArrayList<>();
            for (Entity entity : l.getAllEntities()) {
                if (entity instanceof ItemEntity) items.add(entity);
            }
            for (Entity entity : items) entity.discard();
        }

        restoreCrates(level);

        List<BlockPos> lobbies = SpawnPointManager.get(level, SpawnPointManager.Type.LOBBY);
        int index = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.getInventory().clearContent();
            player.removeAllEffects();
            player.clearFire();
            player.setGameMode(GameType.SURVIVAL);
            if (!player.isAlive()) continue; // respawns normally

            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(5.0F);
            if (!lobbies.isEmpty()) {
                BlockPos lobby = lobbies.get(index++ % lobbies.size());
                player.teleportTo(level, lobby.getX() + 0.5, lobby.getY(), lobby.getZ() + 0.5,
                        Set.of(), player.getYRot(), player.getXRot(), true);
            }
        }
        if (lobbies.isEmpty()) {
            server.getPlayerList().broadcastSystemMessage(Component.literal(
                    "MANIK: на карті немає блока лоббі, тому гравців не телепортовано."), false);
        }
    }

    /** Puts the flare crates back into their room (picked-up crates leave an empty spot). */
    public static void restoreCrates(ServerLevel level) {
        for (BlockPos pos : SpawnPointManager.get(level, SpawnPointManager.Type.CRATE)) {
            if (level.getBlockState(pos).is(Blocks.AIR)) {
                level.setBlockAndUpdate(pos, ManikBlocks.FLARE_CRATE.get().defaultBlockState());
            }
        }
    }

    private RoundCleanup() {}
}
