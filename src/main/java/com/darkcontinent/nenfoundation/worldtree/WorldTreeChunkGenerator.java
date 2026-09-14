package com.darkcontinent.nenfoundation.worldtree;

import com.darkcontinent.nenfoundation.worldtree.generation.WorldTreeTrunkGenerator;
import com.darkcontinent.nenfoundation.worldtree.generation.WorldTreeHollowGenerator;
import com.darkcontinent.nenfoundation.worldtree.generation.WorldTreeBranchGenerator;
import com.darkcontinent.nenfoundation.worldtree.generation.WorldTreeCanopyGenerator;
import com.darkcontinent.nenfoundation.worldtree.generation.WorldTreeFloraGenerator;
import com.darkcontinent.nenfoundation.worldtree.generation.WorldTreeCrownGenerator;
import com.darkcontinent.nenfoundation.worldtree.checkpoint.WorldTreeCheckpointGenerator;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.block.Blocks;

/**
 * Gerador vazio da dimensao World Tree, usado pelo slice tecnico M0.
 *
 * <p>DECISAO DE M0: a dimensao precisa carregar e aceitar teleporte antes de
 * qualquer bloco ou algoritmo de arvore existir. Retornar chunks vazios aqui
 * separa o risco de registro/dimension type do risco de worldgen procedural.
 */
public final class WorldTreeChunkGenerator extends ChunkGenerator {
    private transient long cachedLayoutSeed = Long.MIN_VALUE;
    private transient WorldTreeLayout cachedLayout;

    public static final MapCodec<WorldTreeChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source")
                            .forGetter(generator -> generator.biomeSource))
                    .apply(instance, instance.stable(WorldTreeChunkGenerator::new)));

    public WorldTreeChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random,
            BiomeManager biomeManager, StructureManager structureManager,
            ChunkAccess chunk, GenerationStep.Carving step) {
        // M0 e um vazio deliberado: nenhum carver deve criar terreno por acidente.
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager,
            RandomState random, ChunkAccess chunk) {
        // O layout e imutavel para a seed: gerar uma vez evita reconstruir o grafo
        // de galhos e anchors para cada chunk solicitado pela dimensao.
        WorldTreeLayout layout = layoutFor(level.getSeed());
        WorldTreeTrunkGenerator.generate(chunk, layout);
        WorldTreeBranchGenerator.generate(chunk, layout);
        WorldTreeCrownGenerator.generate(chunk, layout);
        // O CHECKPOINT VEM ANTES DA COPA, e a ordem aqui e semantica.
        //
        // A plataforma do checkpoint so preenche AR, e a copa tambem. Com a copa
        // primeiro, cada folha ja posta virava um buraco na plataforma: a varanda
        // saia como peneira, e mais furada quanto mais alto o checkpoint --
        // CROWN e SUMMIT ficam debaixo dos discos do lider central, que tem raio
        // de ate 168. Era a "base totalmente bugada" vista em jogo, e nao havia
        // erro nenhum: dois geradores educados, cada um respeitando o que o outro
        // ja tinha escrito.
        //
        // Invertida, a copa e que contorna a plataforma -- que e a regra ja
        // escrita para madeira em WorldTreeCanopyGenerator: folha nao sobrescreve
        // estrutura.
        WorldTreeCheckpointGenerator.generate(chunk, layout);
        WorldTreeCanopyGenerator.generate(chunk, layout);
        WorldTreeHollowGenerator.generate(chunk, layout);
        WorldTreeFloraGenerator.generate(chunk, layout);
    }

    private synchronized WorldTreeLayout layoutFor(long seed) {
        if (cachedLayout == null || cachedLayoutSeed != seed) {
            cachedLayout = WorldTreeLayoutGenerator.generate(seed, 0, 0);
            cachedLayoutSeed = seed;
        }
        return cachedLayout;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender,
            RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
        // A dimensao tecnica nao possui fauna nem spawn natural.
    }

    @Override
    public int getGenDepth() {
        return 1536;
    }

    @Override
    public int getSeaLevel() {
        return -63;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type,
            LevelHeightAccessor level, RandomState random) {
        return level.getMinBuildHeight();
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level,
            RandomState random) {
        var states = new net.minecraft.world.level.block.state.BlockState[level.getHeight()];
        Arrays.fill(states, Blocks.AIR.defaultBlockState());
        return new NoiseColumn(level.getMinBuildHeight(), states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        info.add("World Tree M0: empty generator");
    }
}
