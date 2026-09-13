package com.darkcontinent.nenfoundation.structure;

import com.darkcontinent.nenfoundation.NenFoundation;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.function.ToIntBiFunction;

/** Coloca todos os placements do posto no lado servidor. */
public final class PostoAvancadoWorldPlacer {
    private static final ResourceKey<net.minecraft.world.level.storage.loot.LootTable>
            SUPRIMENTOS_POSTO = ResourceKey.create(Registries.LOOT_TABLE,
                    NenFoundation.id("chests/hunter_outpost_supply"));
    private static final ResourceKey<net.minecraft.world.level.storage.loot.LootTable>
            SEGREDO_POSTO = ResourceKey.create(Registries.LOOT_TABLE,
                    NenFoundation.id("chests/hunter_outpost_secret"));

    private PostoAvancadoWorldPlacer() { }

    public static Resultado colocar(ServerLevel level, int centroX, int centroZ,
            PostoAvancadoPlacementTransform.Rotacao rotacao) {
        Objects.requireNonNull(level, "nivel ausente");
        return colocar(level, centroX, centroZ, rotacao,
                (x, z) -> level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1);
    }

    /**
     * O mesmo, com a altura do terreno vindo de fora.
     *
     * <p>ELE EXISTE PARA O GAMETEST, e o motivo vale escrever. A versao que le a
     * altura do nivel colocava o posto em coordenadas fixas do mundo e
     * dependia de <b>a terra ali ser plana por sorte</b>: o
     * {@code runGameTestServer} gera um mundo a cada execucao, e a variacao
     * medida no ponto escolhido foi 6 contra um limite de 5. O teste reprovava
     * sem nada ter mudado no codigo que ele testa.
     *
     * <p>Teste que depende de geracao de mundo em coordenada fixa nao e um
     * teste ruim de vez em quando -- ele e um teste que <b>as vezes</b> diz a
     * verdade, e nao ha como saber qual das vezes foi.
     *
     * <p>A producao continua chamando a versao de cima, sem mudanca nenhuma de
     * comportamento: o que este metodo separa e "onde esta o chao" de "escreva
     * os blocos", que sao duas perguntas diferentes e so uma delas e deste
     * arquivo.
     */
    public static Resultado colocar(ServerLevel level, int centroX, int centroZ,
            PostoAvancadoPlacementTransform.Rotacao rotacao,
            ToIntBiFunction<Integer, Integer> alturaDoTerreno) {
        Objects.requireNonNull(level, "nivel ausente");
        Objects.requireNonNull(alturaDoTerreno, "altura do terreno ausente");
        if (!level.getBiome(new BlockPos(centroX, level.getMinBuildHeight(), centroZ))
                .is(PostoAvancadoBiomes.HUNTER_OUTPOST_BIOMES)) {
            return Resultado.rejeitado(Resultado.Motivo.BIOMA);
        }
        var plano = PostoAvancadoPlacementPlanner.planejar(centroX, centroZ, rotacao,
                alturaDoTerreno);
        if (plano.isEmpty()) {
            return Resultado.rejeitado(Resultado.Motivo.TERRENO);
        }
        var palette = PostoAvancadoMaterialPalette.vanilla();
        Set<BlockPos> unicos = new HashSet<>();
        for (var placement : plano.orElseThrow().placements()) {
            BlockState estado = BuiltInRegistries.BLOCK
                    .get(palette.get(placement.material()))
                    .defaultBlockState();
            BlockPos posicao = placement.posicao();
            if (unicos.add(posicao)) {
                level.setBlock(posicao, estado, 3);
                if (placement.material() == PostoAvancadoBlockout.Material.CRATE
                        || placement.material() == PostoAvancadoBlockout.Material.SECRET_CACHE) {
                    if (level.getBlockEntity(posicao) instanceof ChestBlockEntity chest) {
                        var tabela = placement.material() == PostoAvancadoBlockout.Material.SECRET_CACHE
                                ? SEGREDO_POSTO : SUPRIMENTOS_POSTO;
                        chest.setLootTable(tabela, level.getRandom().nextLong());
                    }
                }
            }
        }
        // Glass panes depend on their neighbours for the final shape. The
        // blockout is emitted in one pass, so recalculate the window states
        // after every adjacent wall/pane already exists.
        for (var placement : plano.orElseThrow().placements()) {
            if (placement.material() == PostoAvancadoBlockout.Material.WINDOW) {
                BlockPos posicao = placement.posicao();
                BlockState estado = level.getBlockState(posicao);
                level.setBlock(posicao, Block.updateFromNeighbourShapes(estado, level, posicao), 3);
            }
        }
        return new Resultado(plano.orElseThrow().placements().size(), unicos.size(),
                plano.orElseThrow().origem(), plano.orElseThrow().terreno().diferenca(),
                Resultado.Motivo.ACEITO);
    }

    public record Resultado(int placements, int blocosUnicos, BlockPos origem, int variacaoTerreno,
            Motivo motivo) {
        private static Resultado rejeitado(Motivo motivo) {
            return new Resultado(0, 0, BlockPos.ZERO, -1, motivo);
        }

        public Resultado {
            origem = origem.immutable();
            Objects.requireNonNull(motivo, "motivo ausente");
        }

        public boolean rejeitado() {
            return motivo != Motivo.ACEITO;
        }

        public enum Motivo { ACEITO, BIOMA, TERRENO }
    }
}
