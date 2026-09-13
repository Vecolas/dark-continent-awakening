package com.darkcontinent.nenfoundation.worldtree.checkpoint;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeDebugCommands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;

/** Regras server-side de descoberta e futuro fast travel. */
public final class WorldTreeCheckpointService {
    private WorldTreeCheckpointService() {
    }

    public static void interact(ServerPlayer player, BlockPos anchorPos) {
        WorldTreeCheckpoint checkpoint = checkpointForAnchor(player, anchorPos);
        if (checkpoint == null) {
            player.displayClientMessage(Component.literal("Este anchor não pertence à rota da World Tree."), true);
            return;
        }
        WorldTreePlayerProgressSavedData progress = progress(player.serverLevel());
        if (progress.unlock(player.getUUID(), checkpoint)) {
            player.displayClientMessage(Component.literal(
                    "Hunter Climbing Anchor desbloqueado: " + checkpoint.id()), false);
        } else {
            player.displayClientMessage(Component.literal(
                    "Anchor ativo: " + checkpoint.id()), false);
        }
        showDestinations(player, progress);
    }

    private static WorldTreeCheckpoint checkpointForAnchor(ServerPlayer player, BlockPos pos) {
        if (player.level().dimension() == Level.OVERWORLD) {
            var data = player.serverLevel().getDataStorage().computeIfAbsent(
                    com.darkcontinent.nenfoundation.worldtree.WorldTreeSavedData.factory(),
                    com.darkcontinent.nenfoundation.worldtree.WorldTreeSavedData.DATA_ID);
            data.initialize(player.serverLevel().getSeed());
            return Math.hypot(pos.getX() - data.overworldOriginX(),
                    pos.getZ() - data.overworldOriginZ()) <= 60.0
                    ? WorldTreeCheckpoint.BASE : null;
        }
        if (player.level().dimension() != WorldTreeDebugCommands.WORLD_TREE_LEVEL) {
            return null;
        }
        return WorldTreeCheckpoint.nearest(pos.getY());
    }

    private static void showDestinations(ServerPlayer player,
            WorldTreePlayerProgressSavedData progress) {
        Component prefix = Component.literal("Destinos desbloqueados: ");
        player.sendSystemMessage(prefix);
        for (WorldTreeCheckpoint checkpoint : progress.unlocked(player.getUUID())) {
            Component destination = Component.literal("[" + checkpoint.id() + "]")
                    .withStyle(Style.EMPTY.withClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            "/hxh worldtree travel " + checkpoint.id())));
            player.sendSystemMessage(destination);
        }
    }

    public static boolean canTravel(ServerPlayer player, WorldTreeCheckpoint target) {
        return isNearAnchor(player) && progress(player.serverLevel())
                .isUnlocked(player.getUUID(), target);
    }

    public static boolean travelTo(ServerPlayer player, WorldTreeCheckpoint target) {
        if (!canTravel(player, target)) {
            return false;
        }
        if (target == WorldTreeCheckpoint.BASE) {
            ServerLevel overworld = player.getServer().overworld();
            var data = overworld.getDataStorage().computeIfAbsent(
                    com.darkcontinent.nenfoundation.worldtree.WorldTreeSavedData.factory(),
                    com.darkcontinent.nenfoundation.worldtree.WorldTreeSavedData.DATA_ID);
            data.initialize(overworld.getSeed());
            player.teleportTo(overworld, data.overworldOriginX() + 49.5D, 80.0D,
                    data.overworldOriginZ() + 0.5D, player.getYRot(), player.getXRot());
            return true;
        }
        ServerLevel destination = player.getServer().getLevel(WorldTreeDebugCommands.WORLD_TREE_LEVEL);
        if (destination == null) {
            return false;
        }
        double radius = Math.max(18.0D, com.darkcontinent.nenfoundation.worldtree.WorldTreeLayoutGenerator
                .generate(destination.getSeed(), 0, 0).trunk()
                .radiusAt(Math.min(target.y(), 1200)));
        player.teleportTo(destination, radius + 1.5D, target.y(), 0.5D,
                player.getYRot(), player.getXRot());
        return true;
    }

    public static boolean isNearAnchor(ServerPlayer player) {
        if (player.level().dimension() == Level.OVERWORLD) {
            var data = player.serverLevel().getDataStorage().computeIfAbsent(
                    com.darkcontinent.nenfoundation.worldtree.WorldTreeSavedData.factory(),
                    com.darkcontinent.nenfoundation.worldtree.WorldTreeSavedData.DATA_ID);
            data.initialize(player.serverLevel().getSeed());
            return player.distanceToSqr(data.overworldOriginX() + 49.5D, 80.0D,
                    data.overworldOriginZ() + 0.5D) <= 64.0D;
        }
        if (player.level().dimension() != WorldTreeDebugCommands.WORLD_TREE_LEVEL) {
            return false;
        }
        int y = player.blockPosition().getY();
        WorldTreeCheckpoint checkpoint = WorldTreeCheckpoint.nearest(y);
        if (checkpoint == null) {
            return false;
        }
        return player.getX() >= 16 && player.getX() <= 60
                && Math.abs(player.getZ()) <= 8;
    }

    private static WorldTreePlayerProgressSavedData progress(ServerLevel anyLevel) {
        ServerLevel overworld = anyLevel.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
                WorldTreePlayerProgressSavedData.factory(),
                WorldTreePlayerProgressSavedData.DATA_ID);
    }
}
