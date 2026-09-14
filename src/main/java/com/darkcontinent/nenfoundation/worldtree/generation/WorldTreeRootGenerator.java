package com.darkcontinent.nenfoundation.worldtree.generation;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeBlocks;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayout;
import com.darkcontinent.nenfoundation.worldtree.WorldTreePoint;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeSpline;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkAccess;

/** Gera as splines de raizes no espaco do Overworld, chunk a chunk. */
public final class WorldTreeRootGenerator {
    private static final int SAMPLE_COUNT = 40;
    private static final double MAX_ROOT_REACH = 145.0;

    private WorldTreeRootGenerator() {
    }

    public static void generate(ChunkAccess chunk, WorldTreeLayout layout, int baseY) {
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int maxX = minX + 16;
        int maxZ = minZ + 16;
        int originX = layout.overworldOriginX();
        int originZ = layout.overworldOriginZ();
        if (!chunkIntersectsRoots(minX, maxX, minZ, maxZ, originX, originZ)) {
            return;
        }

        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        for (int rootId = 0; rootId < layout.roots().size(); rootId++) {
            WorldTreeSpline spline = layout.roots().get(rootId);
            for (int sample = 0; sample <= SAMPLE_COUNT; sample++) {
                double t = (double) sample / SAMPLE_COUNT;
                WorldTreePoint point = bezier(spline, t);
                double radius = radiusAt(spline, t);
                int fromX = Math.max(minX, (int) Math.floor(originX + point.x() - radius));
                int toX = Math.min(maxX, (int) Math.ceil(originX + point.x() + radius) + 1);
                int fromZ = Math.max(minZ, (int) Math.floor(originZ + point.z() - radius));
                int toZ = Math.min(maxZ, (int) Math.ceil(originZ + point.z() + radius) + 1);
                int centerY = baseY + (int) Math.round(point.y() - 48.0);
                for (int x = fromX; x < toX; x++) {
                    for (int z = fromZ; z < toZ; z++) {
                        double horizontal = Math.hypot(x - originX - point.x(), z - originZ - point.z());
                        if (horizontal > radius) {
                            continue;
                        }
                        int bottom = Math.max(chunk.getMinBuildHeight(), centerY - (int) Math.ceil(radius));
                        int top = Math.min(chunk.getMaxBuildHeight(), centerY + (int) Math.ceil(radius) + 1);
                        for (int y = bottom; y < top; y++) {
                            double distance = Math.sqrt(horizontal * horizontal + Math.pow(y - centerY, 2));
                            if (distance <= radius) {
                                position.set(x, y, z);
                                chunk.setBlockState(position, rootState(rootId, x, y, z, layout.seed()), false);
                            }
                        }
                    }
                }
            }
        }
    }

    static boolean chunkIntersectsRoots(int minX, int maxX, int minZ, int maxZ, int originX, int originZ) {
        return minX <= originX + MAX_ROOT_REACH && maxX >= originX - MAX_ROOT_REACH
                && minZ <= originZ + MAX_ROOT_REACH && maxZ >= originZ - MAX_ROOT_REACH;
    }

    // A CURVA MORA EM `WorldTreeSpline`, e nao aqui. Estes dois delegam para
    // que os chamadores antigos continuem funcionando sem duas contas da mesma
    // curva no repositorio.
    static WorldTreePoint bezier(WorldTreeSpline spline, double t) {
        return spline.pointAt(t);
    }

    static double radiusAt(WorldTreeSpline spline, double t) {
        return spline.radiusAt(t);
    }

    private static net.minecraft.world.level.block.state.BlockState rootState(
            int rootId, int x, int y, int z, long seed) {
        long value = seed ^ ((long) rootId * 341873128712L) ^ ((long) x * 132897987541L)
                ^ ((long) y * 42317861L) ^ z;
        return Math.floorMod(value, 11) == 0
                ? WorldTreeBlocks.WORLD_TREE_ROOT_MOSSY.get().defaultBlockState()
                : WorldTreeBlocks.WORLD_TREE_ROOT.get().defaultBlockState();
    }
}
