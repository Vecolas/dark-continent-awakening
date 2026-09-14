package com.darkcontinent.nenfoundation.worldtree.generation;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeHollow;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayout;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeTrunkProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;

/** Abre a rota principal e pockets sem romper a casca externa do tronco. */
public final class WorldTreeHollowGenerator {
    private static final double ROUTE_RADIUS = 3.5;

    private WorldTreeHollowGenerator() {
    }

    public static void generate(ChunkAccess chunk, WorldTreeLayout layout) {
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int maxX = minX + 16;
        int maxZ = minZ + 16;
        if (!WorldTreeTrunkGenerator.chunkIntersectsTrunk(minX, maxX, minZ, maxZ)) {
            return;
        }
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        for (int y = layout.trunk().baseY(); y < layout.trunk().topY(); y++) {
            double routeX = routeX(y, layout.seed());
            double routeZ = routeZ(y, layout.seed());
            carveBox(chunk, position, minX, maxX, minZ, maxZ, y, routeX, routeZ,
                    ROUTE_RADIUS, ROUTE_RADIUS, layout);
        }
        for (WorldTreeHollow hollow : layout.hollows()) {
            int minY = Math.max(layout.trunk().baseY(), (int) Math.floor(hollow.centerY() - hollow.radiusY()));
            int maxY = Math.min(layout.trunk().topY(), (int) Math.ceil(hollow.centerY() + hollow.radiusY()) + 1);
            for (int y = minY; y < maxY; y++) {
                carveBox(chunk, position, minX, maxX, minZ, maxZ, y, hollow.centerX(), hollow.centerZ(),
                        hollow.radiusX(), hollow.radiusZ(), layout, hollow);
            }
        }
    }

    static double routeX(int y, long seed) {
        return Math.sin(y * 0.011 + seed * 0.0000013) * 5.5;
    }

    static double routeZ(int y, long seed) {
        return Math.cos(y * 0.009 - seed * 0.0000017) * 5.5;
    }

    private static void carveBox(ChunkAccess chunk, BlockPos.MutableBlockPos position,
            int minX, int maxX, int minZ, int maxZ, int y, double centerX, double centerZ,
            double radiusX, double radiusZ, WorldTreeLayout layout) {
        carveBox(chunk, position, minX, maxX, minZ, maxZ, y, centerX, centerZ,
                radiusX, radiusZ, layout, null);
    }

    private static void carveBox(ChunkAccess chunk, BlockPos.MutableBlockPos position,
            int minX, int maxX, int minZ, int maxZ, int y, double centerX, double centerZ,
            double radiusX, double radiusZ, WorldTreeLayout layout, WorldTreeHollow hollow) {
        int fromX = Math.max(minX, (int) Math.floor(centerX - radiusX - 1.0));
        int toX = Math.min(maxX, (int) Math.ceil(centerX + radiusX + 1.0));
        int fromZ = Math.max(minZ, (int) Math.floor(centerZ - radiusZ - 1.0));
        int toZ = Math.min(maxZ, (int) Math.ceil(centerZ + radiusZ + 1.0));
        WorldTreeTrunkProfile trunk = layout.trunk();
        for (int x = fromX; x < toX; x++) {
            for (int z = fromZ; z < toZ; z++) {
                double dx = x - centerX;
                double dz = z - centerZ;
                boolean inside = hollow == null
                        ? dx * dx + dz * dz <= radiusX * radiusX
                        : hollow.contains(x, y, z);
                if (!inside) {
                    continue;
                }
                double trunkRadius = WorldTreeTrunkGenerator.irregularRadius(
                        trunk.radiusAt(y), x, y, z, layout.seed());
                if (Math.sqrt(x * (double) x + z * (double) z) + 0.75 >= trunkRadius) {
                    continue;
                }
                position.set(x, y, z);
                chunk.setBlockState(position, Blocks.AIR.defaultBlockState(), false);
            }
        }
    }
}
