package com.darkcontinent.nenfoundation.worldtree.checkpoint;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeDebugCommands;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayout;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayoutGenerator;
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
            var posto = com.darkcontinent.nenfoundation.worldtree.generation
                    .WorldTreeBaseGenerator.postoDoPe(
                            WorldTreeLayoutGenerator.generate(overworld.getSeed(), 0, 0));
            player.teleportTo(overworld, posto.spawnX(), posto.floorY() + 1,
                    posto.spawnZ(), player.getYRot(), player.getXRot());
            return true;
        }
        ServerLevel destination = player.getServer().getLevel(WorldTreeDebugCommands.WORLD_TREE_LEVEL);
        if (destination == null) {
            return false;
        }
        WorldTreeLayout layout = WorldTreeLayoutGenerator.generate(destination.getSeed(), 0, 0);
        // PELO CACHE, e nao por uma busca solta: `forCheckpoint` precisa do plano
        // de copa para saber a altura da torre, e recalcular os dois aqui seria
        // refazer todo o trabalho a cada viagem.
        var posto = WorldTreeClimbingPosts.of(layout).get(target.ordinal());
        player.teleportTo(destination, posto.spawnX(), posto.floorY() + 1, posto.spawnZ(),
                player.getYRot(), player.getXRot());
        return true;
    }

    public static boolean isNearAnchor(ServerPlayer player) {
        if (player.level().dimension() == Level.OVERWORLD) {
            var data = player.serverLevel().getDataStorage().computeIfAbsent(
                    com.darkcontinent.nenfoundation.worldtree.WorldTreeSavedData.factory(),
                    com.darkcontinent.nenfoundation.worldtree.WorldTreeSavedData.DATA_ID);
            data.initialize(player.serverLevel().getSeed());
            var posto = com.darkcontinent.nenfoundation.worldtree.generation
                    .WorldTreeBaseGenerator.postoDoPe(
                            WorldTreeLayoutGenerator.generate(
                                    player.serverLevel().getSeed(), 0, 0));
            return player.distanceToSqr(posto.spawnX(), posto.floorY() + 1,
                    posto.spawnZ()) <= 64.0D;
        }
        if (player.level().dimension() != WorldTreeDebugCommands.WORLD_TREE_LEVEL) {
            return false;
        }
        int y = player.blockPosition().getY();
        WorldTreeCheckpoint checkpoint = WorldTreeCheckpoint.nearest(y);
        if (checkpoint == null) {
            return false;
        }
        WorldTreeLayout layout = WorldTreeLayoutGenerator.generate(player.serverLevel().getSeed(), 0, 0);
        var posto = WorldTreeClimbingPosts.of(layout).get(checkpoint.ordinal());
        double dx = player.getX() - posto.spawnX();
        double dz = player.getZ() - posto.spawnZ();
        return dx * dx + dz * dz <= 64.0D;
    }

    private static WorldTreePlayerProgressSavedData progress(ServerLevel anyLevel) {
        ServerLevel overworld = anyLevel.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
                WorldTreePlayerProgressSavedData.factory(),
                WorldTreePlayerProgressSavedData.DATA_ID);
    }
}
