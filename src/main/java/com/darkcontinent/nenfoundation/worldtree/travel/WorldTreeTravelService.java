package com.darkcontinent.nenfoundation.worldtree.travel;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeDebugCommands;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeSavedData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** Viagem server-authoritative entre a base e a dimensao da World Tree. */
public final class WorldTreeTravelService {
    private static final int COOLDOWN_TICKS = 60;
    private static final int BASE_TRANSITION_Y = 284;
    /** Altura em que o portal do Overworld reencontra a continuação da árvore. */
    private static final int WORLD_TREE_ENTRY_Y = 200;
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();

    private WorldTreeTravelService() {
    }

    public static void tryTravel(ServerPlayer player) {
        if (isCoolingDown(player)) {
            return;
        }
        if (player.level().dimension() == Level.OVERWORLD) {
            tryEnterWorldTree(player);
        } else if (player.level().dimension() == WorldTreeDebugCommands.WORLD_TREE_LEVEL) {
            tryReturnToOverworld(player);
        }
    }

    static boolean isOverworldGate(double x, double y, double z, int originX, int originZ) {
        return y >= BASE_TRANSITION_Y - 12 && y <= BASE_TRANSITION_Y + 26
                && Math.hypot(x - originX, z - originZ) <= 10.0;
    }

    static boolean isWorldTreeGate(double x, double y, double z) {
        return y <= 52.0 && y >= 40.0 && Math.hypot(x, z) <= 12.0;
    }

    static int baseTransitionY() {
        return BASE_TRANSITION_Y;
    }

    static int worldTreeEntryY() {
        return WORLD_TREE_ENTRY_Y;
    }

    private static void tryEnterWorldTree(ServerPlayer player) {
        ServerLevel overworld = player.serverLevel();
        WorldTreeSavedData data = overworld.getDataStorage().computeIfAbsent(
                WorldTreeSavedData.factory(), WorldTreeSavedData.DATA_ID);
        data.initialize(overworld.getSeed());
        if (!isOverworldGate(player.getX(), player.getY(), player.getZ(),
                data.overworldOriginX(), data.overworldOriginZ())) {
            return;
        }
        ServerLevel destination = player.getServer().getLevel(WorldTreeDebugCommands.WORLD_TREE_LEVEL);
        if (destination == null) {
            return;
        }
        double localX = player.getX() - data.overworldOriginX();
        double localZ = player.getZ() - data.overworldOriginZ();
        // A entrada reencontra a árvore em altura intermediária. O portal
        // inferior da dimensão continua em Y=48 para preservar a rota já
        // escalada e o caminho de retorno ao Overworld.
        teleport(player, destination, localX, WORLD_TREE_ENTRY_Y, localZ);
        data.markDiscovered();
    }

    private static void tryReturnToOverworld(ServerPlayer player) {
        if (!isWorldTreeGate(player.getX(), player.getY(), player.getZ())) {
            return;
        }
        ServerLevel overworld = player.getServer().overworld();
        WorldTreeSavedData data = overworld.getDataStorage().computeIfAbsent(
                WorldTreeSavedData.factory(), WorldTreeSavedData.DATA_ID);
        data.initialize(overworld.getSeed());
        teleport(player, overworld, data.overworldOriginX() + player.getX(),
                BASE_TRANSITION_Y + (player.getY() - 48.0),
                data.overworldOriginZ() + player.getZ());
    }

    private static void teleport(ServerPlayer player, ServerLevel destination,
            double x, double y, double z) {
        long now = player.serverLevel().getGameTime();
        COOLDOWNS.put(player.getUUID(), now + COOLDOWN_TICKS);
        player.teleportTo(destination, x, y, z, player.getYRot(), player.getXRot());
    }

    private static boolean isCoolingDown(ServerPlayer player) {
        Long until = COOLDOWNS.get(player.getUUID());
        if (until == null) {
            return false;
        }
        if (player.serverLevel().getGameTime() >= until) {
            COOLDOWNS.remove(player.getUUID());
            return false;
        }
        return true;
    }
}
