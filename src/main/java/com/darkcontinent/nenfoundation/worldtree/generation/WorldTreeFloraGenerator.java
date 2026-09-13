package com.darkcontinent.nenfoundation.worldtree.generation;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayout;
import com.darkcontinent.nenfoundation.worldtree.WorldTreePoint;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeSpline;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;

/** Flora deterministica para transformar galhos e clareiras em habitat vivo. */
public final class WorldTreeFloraGenerator {
    private static final int SAMPLES_PER_BRANCH = 48;

    private WorldTreeFloraGenerator() {
    }

    public static void generate(ChunkAccess chunk, WorldTreeLayout layout) {
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int maxX = minX + 16;
        int maxZ = minZ + 16;
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();

        for (int branchId = 0; branchId < layout.branches().size(); branchId++) {
            WorldTreeSpline branch = layout.branches().get(branchId);
            for (int sample = 4; sample < SAMPLES_PER_BRANCH; sample += 2) {
                double t = (double) sample / SAMPLES_PER_BRANCH;
                WorldTreePoint point = WorldTreeBranchGenerator.bezier(branch, t);
                double radius = WorldTreeBranchGenerator.radiusAt(branch, t);
                if (point.x() < minX - radius || point.x() > maxX + radius
                        || point.z() < minZ - radius || point.z() > maxZ + radius
                        || point.y() < 520.0) {
                    continue;
                }
                int topY = (int) Math.ceil(point.y() + radius * 0.72) + 1;
                int flowerCount = 1 + Math.floorMod(mix(layout.seed(), branchId, sample), 3);
                for (int flower = 0; flower < flowerCount; flower++) {
                    long value = mix(layout.seed() ^ 0x6A09E667F3BCC909L,
                            branchId * 97 + sample * 11 + flower,
                            topY);
                    int x = (int) Math.round(point.x()) + Math.floorMod(value, 5) - 2;
                    int z = (int) Math.round(point.z()) + Math.floorMod(value >>> 8, 5) - 2;
                    if (x < minX || x >= maxX || z < minZ || z >= maxZ) {
                        continue;
                    }
                    placeFlower(chunk, position, x, topY, z, flowerState(value, topY));
                }
            }
        }
    }

    static void placeFlower(ChunkAccess chunk, BlockPos.MutableBlockPos position,
            int x, int y, int z, net.minecraft.world.level.block.state.BlockState flower) {
        if (y <= chunk.getMinBuildHeight() || y >= chunk.getMaxBuildHeight()) {
            return;
        }
        position.set(x, y, z);
        if (!chunk.getBlockState(position).isAir()) {
            return;
        }
        position.set(x, y - 1, z);
        if (!chunk.getBlockState(position).isSolid()) {
            return;
        }
        position.set(x, y, z);
        chunk.setBlockState(position, flower, false);
    }

    static net.minecraft.world.level.block.state.BlockState flowerState(long value, int y) {
        Block flower = switch (Math.floorMod(value + y, 7)) {
            case 0 -> Blocks.POPPY;
            case 1 -> Blocks.DANDELION;
            case 2 -> Blocks.CORNFLOWER;
            case 3 -> Blocks.AZURE_BLUET;
            case 4 -> Blocks.OXEYE_DAISY;
            case 5 -> Blocks.ALLIUM;
            default -> Blocks.LILY_OF_THE_VALLEY;
        };
        return flower.defaultBlockState();
    }

    private static long mix(long seed, int x, int z) {
        long value = seed ^ ((long) x * 341873128712L) ^ ((long) z * 132897987541L);
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
