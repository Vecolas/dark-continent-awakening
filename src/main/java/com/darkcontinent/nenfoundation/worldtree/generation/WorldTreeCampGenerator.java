package com.darkcontinent.nenfoundation.worldtree.generation;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;

/** Blockout procedural do Hunter Base Camp, limitado ao chunk atual. */
public final class WorldTreeCampGenerator {
    private static final int CAMP_OFFSET_X = 72;
    private static final int CAMP_HALF_WIDTH = 12;
    private static final int CAMP_HALF_DEPTH = 9;
    private static final int CAMP_Y = 66;

    private WorldTreeCampGenerator() {
    }

    public static void generateChunk(ChunkAccess chunk, WorldTreeLayout layout) {
        int originX = layout.overworldOriginX() + CAMP_OFFSET_X;
        int originZ = layout.overworldOriginZ();
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int maxX = minX + 16;
        int maxZ = minZ + 16;
        if (!chunkIntersects(minX, maxX, minZ, maxZ, originX, originZ)) {
            return;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = Math.max(minX, originX - CAMP_HALF_WIDTH);
                x <= Math.min(maxX - 1, originX + CAMP_HALF_WIDTH); x++) {
            for (int z = Math.max(minZ, originZ - CAMP_HALF_DEPTH);
                    z <= Math.min(maxZ - 1, originZ + CAMP_HALF_DEPTH); z++) {
                pos.set(x, CAMP_Y - 1, z);
                if (chunk.getBlockState(pos).isAir()) {
                    chunk.setBlockState(pos, Blocks.COARSE_DIRT.defaultBlockState(), false);
                }
            }
        }
        place(chunk, pos, originX, CAMP_Y, originZ, Blocks.CRAFTING_TABLE);
        place(chunk, pos, originX + 2, CAMP_Y, originZ, Blocks.BARREL);
        place(chunk, pos, originX - 4, CAMP_Y, originZ, Blocks.CAMPFIRE);
        for (int dx : new int[] {-9, -2, 5, 9}) {
            place(chunk, pos, originX + dx, CAMP_Y, originZ - 5, Blocks.OAK_LOG);
            place(chunk, pos, originX + dx, CAMP_Y + 1, originZ - 5, Blocks.WHITE_WOOL);
        }
        for (int dx : new int[] {-9, -2, 5, 9}) {
            place(chunk, pos, originX + dx, CAMP_Y, originZ + 5, Blocks.OAK_LOG);
            place(chunk, pos, originX + dx, CAMP_Y + 1, originZ + 5, Blocks.WHITE_WOOL);
        }
    }

    static boolean chunkIntersects(int minX, int maxX, int minZ, int maxZ,
            int originX, int originZ) {
        return minX <= originX + CAMP_HALF_WIDTH && maxX >= originX - CAMP_HALF_WIDTH
                && minZ <= originZ + CAMP_HALF_DEPTH && maxZ >= originZ - CAMP_HALF_DEPTH;
    }

    private static void place(ChunkAccess chunk, BlockPos.MutableBlockPos pos,
            int x, int y, int z, net.minecraft.world.level.block.Block block) {
        if (x < chunk.getPos().getMinBlockX() || x >= chunk.getPos().getMinBlockX() + 16
                || z < chunk.getPos().getMinBlockZ() || z >= chunk.getPos().getMinBlockZ() + 16
                || y < chunk.getMinBuildHeight() || y >= chunk.getMaxBuildHeight()) {
            return;
        }
        pos.set(x, y, z);
        if (chunk.getBlockState(pos).isAir()) {
            chunk.setBlockState(pos, block.defaultBlockState(), false);
        }
    }
}
