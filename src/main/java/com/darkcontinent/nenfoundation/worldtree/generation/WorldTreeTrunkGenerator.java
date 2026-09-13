package com.darkcontinent.nenfoundation.worldtree.generation;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeBlocks;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayout;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayoutGenerator;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeTrunkProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/** Gera somente o volume do tronco pertencente ao chunk recebido. */
public final class WorldTreeTrunkGenerator {
    private static final int CHUNK_SIZE = 16;
    private static final double MAX_TRUNK_RADIUS = 40.0;

    private WorldTreeTrunkGenerator() {
    }

    public static void generate(ChunkAccess chunk, long seed) {
        WorldTreeLayout layout = WorldTreeLayoutGenerator.generate(seed, 0, 0);
        generate(chunk, layout);
    }

    public static void generate(ChunkAccess chunk, WorldTreeLayout layout) {
        WorldTreeTrunkProfile trunk = layout.trunk();
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int maxX = minX + CHUNK_SIZE;
        int maxZ = minZ + CHUNK_SIZE;
        if (!chunkIntersectsTrunk(minX, maxX, minZ, maxZ)) {
            return;
        }
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();

        for (int y = trunk.baseY(); y < trunk.topY(); y++) {
            double centerRadius = trunk.radiusAt(y);
            for (int x = minX; x < maxX; x++) {
                for (int z = minZ; z < maxZ; z++) {
                    double radius = irregularRadius(centerRadius, x, y, z, layout.seed());
                    double distance = Math.sqrt((double) x * x + (double) z * z);
                    if (distance > radius) {
                        continue;
                    }

                    BlockState state = stateFor(distance, radius, x, y, z, layout.seed());
                    if (state != null) {
                        position.set(x, y, z);
                        chunk.setBlockState(position, state, false);
                    }
                }
            }
        }
    }

    static boolean chunkIntersectsTrunk(int minX, int maxX, int minZ, int maxZ) {
        return minX <= MAX_TRUNK_RADIUS && maxX >= -MAX_TRUNK_RADIUS
                && minZ <= MAX_TRUNK_RADIUS && maxZ >= -MAX_TRUNK_RADIUS;
    }

    static double irregularRadius(double baseRadius, int x, int y, int z, long seed) {
        double angle = Math.atan2(z, x);
        double lobes = Math.sin(angle * 3.0 + y * 0.013) * 3.0
                + Math.sin(angle * 7.0 - y * 0.021) * 1.5;
        double noise = Math.sin(x * 0.19 + y * 0.037 + seed * 0.0000017)
                * Math.cos(z * 0.17 - y * 0.029 + seed * 0.0000011) * 2.0;
        return Math.max(1.0, baseRadius + lobes + noise);
    }

    private static BlockState stateFor(double distance, double radius, int x, int y, int z, long seed) {
        double normalized = distance / radius;
        if (normalized >= 0.86) {
            return barkVariant(x, y, z, seed);
        }
        if (normalized >= 0.68) {
            return WorldTreeBlocks.WORLD_TREE_SAPWOOD.get().defaultBlockState();
        }

        double cavity = Math.sin(x * 0.11 + y * 0.043 + seed * 0.0000023)
                + Math.cos(z * 0.13 - y * 0.031 + seed * 0.0000019)
                + Math.sin((x + z) * 0.07 + y * 0.017);
        if (normalized < 0.58 && cavity > 0.35 && normalized > 0.20) {
            return null;
        }
        if (normalized < 0.20) {
            return WorldTreeBlocks.WORLD_TREE_CORE.get().defaultBlockState();
        }
        return WorldTreeBlocks.WORLD_TREE_HEARTWOOD.get().defaultBlockState();
    }

    private static BlockState barkVariant(int x, int y, int z, long seed) {
        long value = mix(seed ^ ((long) x * 341873128712L) ^ ((long) y * 132897987541L)
                ^ ((long) z * 42317861L));
        return switch (Math.floorMod(value, 16)) {
            case 0, 1 -> WorldTreeBlocks.WORLD_TREE_BARK_DARK.get().defaultBlockState();
            case 2, 3 -> WorldTreeBlocks.WORLD_TREE_BARK_MOSSY.get().defaultBlockState();
            case 4 -> WorldTreeBlocks.WORLD_TREE_BARK_SCARRED.get().defaultBlockState();
            default -> WorldTreeBlocks.WORLD_TREE_BARK.get().defaultBlockState();
        };
    }

    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
