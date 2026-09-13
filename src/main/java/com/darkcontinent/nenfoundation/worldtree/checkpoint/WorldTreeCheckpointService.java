package com.darkcontinent.nenfoundation.worldtree.checkpoint;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeDebugCommands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** Regras server-side de descoberta e futuro fast travel. */
public final class WorldTreeCheckpointService {
    private WorldTreeCheckpointService() {
    }

    public static void interact(ServerPlayer player) {
        if (player.level().dimension() != WorldTreeDebugCommands.WORLD_TREE_LEVEL) {
            return;
        }
        WorldTreeCheckpoint checkpoint = WorldTreeCheckpoint.nearest(player.blockPosition().getY());
        if (checkpoint == null) {
            player.displayClientMessage(Component.literal("Este anchor não pertence à rota principal."), true);
            return;
        }
        WorldTreePlayerProgressSavedData progress = progress(player.serverLevel());
        if (progress.unlock(player.getUUID(), checkpoint)) {
            player.displayClientMessage(Component.literal(
                    "Hunter Climbing Anchor desbloqueado: " + checkpoint.id()), true);
        } else {
            player.displayClientMessage(Component.literal(
                    "Anchor ativo: " + checkpoint.id()), true);
        }
    }

    public static boolean canTravel(ServerPlayer player, WorldTreeCheckpoint target) {
        return player.level().dimension() == WorldTreeDebugCommands.WORLD_TREE_LEVEL
                && progress(player.serverLevel()).isUnlocked(player.getUUID(), target);
    }

    public static void travelTo(ServerPlayer player, WorldTreeCheckpoint target) {
        if (!canTravel(player, target)) {
            return;
        }
        ServerLevel destination = player.getServer().getLevel(WorldTreeDebugCommands.WORLD_TREE_LEVEL);
        if (destination != null) {
            player.teleportTo(destination, 0.5D, target.y(), 0.5D,
                    player.getYRot(), player.getXRot());
        }
    }

    private static WorldTreePlayerProgressSavedData progress(ServerLevel anyLevel) {
        ServerLevel overworld = anyLevel.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
                WorldTreePlayerProgressSavedData.factory(),
                WorldTreePlayerProgressSavedData.DATA_ID);
    }
}
