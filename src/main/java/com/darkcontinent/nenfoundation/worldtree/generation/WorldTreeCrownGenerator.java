package com.darkcontinent.nenfoundation.worldtree.generation;

import com.darkcontinent.nenfoundation.worldtree.WorldTreeLayout;
import com.darkcontinent.nenfoundation.worldtree.WorldTreePoint;
import com.darkcontinent.nenfoundation.worldtree.WorldTreeTrunkSurface;
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

    /**
     * O perfil do lider MUDOU DE CASA, para {@link WorldTreeTrunkSurface}.
     *
     * <p>Isto e delegado, e nao copia: a cabana dos checkpoints do topo encosta
     * neste perfil, e duas versoes dele divergiriam na primeira correcao de forma
     * -- com a cabana saindo do lugar sem ninguem ter tocado nela.
     */
    public static WorldTreePoint leaderCenter(int y, long seed) {
        return WorldTreeTrunkSurface.leaderCenter(y, seed);
    }

    public static double leaderRadius(int y) {
        return WorldTreeTrunkSurface.leaderRadius(y);
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
