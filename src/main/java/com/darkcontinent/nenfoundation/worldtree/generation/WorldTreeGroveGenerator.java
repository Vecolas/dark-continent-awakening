package com.darkcontinent.nenfoundation.worldtree.generation;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeBlocks;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/** Decoração conservadora do Grove, limitada ao chunk atualmente disponível. */
public final class WorldTreeGroveGenerator {
    private WorldTreeGroveGenerator() {
    }

    public static void generateChunk(ChunkAccess chunk, WorldTreeLayout layout) {
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                if (!WorldTreeBaseStructure.insideGrove(x, z, layout)
                        || !shouldDecorate(layout.seed(), x, z)) {
                    continue;
                }
                int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
                if (y <= chunk.getMinBuildHeight() || y >= chunk.getMaxBuildHeight() - 1) {
                    continue;
                }
                position.set(x, y - 1, z);
                if (!chunk.getBlockState(position).is(BlockTags.DIRT)) {
                    continue;
                }
                position.set(x, y, z);
                if (!chunk.getBlockState(position).isAir()) {
                    continue;
                }
                chunk.setBlockState(position,
                        WorldTreeBlocks.WORLD_TREE_MOSS_CARPET.get().defaultBlockState(), false);
                if (shouldPlaceFlower(layout.seed(), x, z)) {
                    position.set(x, y + 1, z);
                    chunk.setBlockState(position, flowerState(layout.seed(), x, z), false);
                }
                if (shouldPlaceMoss(layout.seed(), x, z)) {
                    position.set(x, y - 1, z);
                    if (chunk.getBlockState(position).is(Blocks.DIRT)) {
                        chunk.setBlockState(position,
                                WorldTreeBlocks.WORLD_TREE_MOSS.get().defaultBlockState(), false);
                    }
                }
            }
        }
    }

    static boolean shouldDecorate(long seed, int x, int z) {
        return Math.floorMod(mix(seed, x, z), 9) == 0;
    }

    static boolean shouldPlaceMoss(long seed, int x, int z) {
        return Math.floorMod(mix(seed ^ 0x51ED2705L, x, z), 5) == 0;
    }

    static boolean shouldPlaceFlower(long seed, int x, int z) {
        return Math.floorMod(mix(seed ^ 0xC0FFEE12L, x, z), 3) != 0;
    }

    private static net.minecraft.world.level.block.state.BlockState flowerState(long seed, int x, int z) {
        return switch (Math.floorMod(mix(seed ^ 0xD1B54A32D192ED03L, x, z), 6)) {
            case 0 -> Blocks.POPPY.defaultBlockState();
            case 1 -> Blocks.DANDELION.defaultBlockState();
            case 2 -> Blocks.CORNFLOWER.defaultBlockState();
            case 3 -> Blocks.AZURE_BLUET.defaultBlockState();
            case 4 -> Blocks.OXEYE_DAISY.defaultBlockState();
            default -> Blocks.ALLIUM.defaultBlockState();
        };
    }

    private static long mix(long seed, int x, int z) {
        long value = seed ^ ((long) x * 341873128712L) ^ ((long) z * 132897987541L);
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
