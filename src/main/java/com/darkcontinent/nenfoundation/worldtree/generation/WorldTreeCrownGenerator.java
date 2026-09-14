package com.darkcontinent.nenfoundation.worldtree.generation;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayout;
import com.darkcontinent.nenfoundation.worldtree.WorldTreePoint;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/** Líder central do topo; os galhos usam o mesmo pipeline em todas as zonas. */
public final class WorldTreeCrownGenerator {
    private static final int LEADER_BOTTOM = 1100;
    private static final int LEADER_TOP = 1450;

    private WorldTreeCrownGenerator() {
    }

    public static void generate(ChunkAccess chunk, WorldTreeLayout layout) {
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int maxX = minX + 16;
        int maxZ = minZ + 16;
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();

        generateLeader(chunk, position, minX, minZ, maxX, maxZ, layout);
    }

    public static WorldTreePoint leaderCenter(int y, long seed) {
        double t = Math.max(0.0, Math.min(1.0, (y - LEADER_BOTTOM)
                / (double) (LEADER_TOP - LEADER_BOTTOM)));
        return new WorldTreePoint(
                Math.sin(y * 0.018 + seed * 0.0000011) * (1.5 + t * 2.5),
                y,
                Math.cos(y * 0.015 - seed * 0.0000017) * (1.5 + t * 2.0));
    }

    public static double leaderRadius(int y) {
        double t = Math.max(0.0, Math.min(1.0, (y - LEADER_BOTTOM)
                / (double) (LEADER_TOP - LEADER_BOTTOM)));
        return 19.0 - t * 12.0;
    }

    private static void generateLeader(ChunkAccess chunk, BlockPos.MutableBlockPos position,
            int minX, int minZ, int maxX, int maxZ, WorldTreeLayout layout) {
        for (int y = LEADER_BOTTOM; y <= LEADER_TOP; y++) {
            WorldTreePoint leader = leaderCenter(y, layout.seed());
            double radius = leaderRadius(y);
            placeEllipsoid(chunk, position, minX, minZ, maxX, maxZ,
                    leader, radius, radius * 0.78, layout.seed());
        }
    }

    private static void placeEllipsoid(ChunkAccess chunk, BlockPos.MutableBlockPos position,
            int minX, int minZ, int maxX, int maxZ, WorldTreePoint center,
            double horizontalRadius, double verticalRadius, long seed) {
        int fromX = Math.max(minX, (int) Math.floor(center.x() - horizontalRadius - 1));
        int toX = Math.min(maxX, (int) Math.ceil(center.x() + horizontalRadius + 1));
        int fromZ = Math.max(minZ, (int) Math.floor(center.z() - horizontalRadius - 1));
        int toZ = Math.min(maxZ, (int) Math.ceil(center.z() + horizontalRadius + 1));
        int fromY = Math.max(chunk.getMinBuildHeight(),
                (int) Math.floor(center.y() - verticalRadius - 1));
        int toY = Math.min(chunk.getMaxBuildHeight(),
                (int) Math.ceil(center.y() + verticalRadius + 1));
        for (int x = fromX; x < toX; x++) {
            for (int z = fromZ; z < toZ; z++) {
                for (int y = fromY; y < toY; y++) {
                    double normalized = Math.pow((x - center.x()) / horizontalRadius, 2.0)
                            + Math.pow((y - center.y()) / verticalRadius, 2.0)
                            + Math.pow((z - center.z()) / horizontalRadius, 2.0);
                    if (normalized <= 1.0) {
                        position.set(x, y, z);
                        BlockState state = WorldTreeTrunkGenerator.stateForNormalized(
                                normalized, x, y, z, seed);
                        if (state != null) {
                            chunk.setBlockState(position, state, false);
                        }
                    }
                }
            }
        }
    }
}
