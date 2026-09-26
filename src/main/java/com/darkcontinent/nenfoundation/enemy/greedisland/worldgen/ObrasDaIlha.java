package com.darkcontinent.nenfoundation.enemy.greedisland.worldgen;

import com.darkcontinent.nenfoundation.enemy.greedisland.GreedIslandRegion;
import com.darkcontinent.nenfoundation.enemy.greedisland.city.DefinicaoDeCidade;
import com.darkcontinent.nenfoundation.enemy.greedisland.city.PlantaDeCidade;
import com.darkcontinent.nenfoundation.enemy.greedisland.city.RegistroDeCidades;
import com.darkcontinent.nenfoundation.enemy.greedisland.landmark.Landmark;
import com.darkcontinent.nenfoundation.enemy.greedisland.landmark.RegistroDeLandmarks;
import com.darkcontinent.nenfoundation.enemy.greedisland.road.TracadoDeEstradas;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * O que a MAO HUMANA deixou na ilha: estradas, cidades e landmarks.
 *
 * <p><b>POR QUE UMA FEATURE, e nao um ChunkGenerator.</b> O terreno de Greed
 * Island ja sai do {@code minecraft:noise} guiado pela
 * {@link FuncaoDeLayout} -- com caverna, minerio, aquifero e regra de
 * superficie de graca. O que falta e o que as pessoas construiram, e isso e
 * decoracao de chunk: roda depois do terreno, sabe onde esta, e escreve so o
 * seu pedaco.
 *
 * <p><b>ELA E CHUNK-LOCAL, e a secao 81 exige isso.</b> Nenhuma chamada aqui
 * olha fora do chunk que esta sendo decorado, e nenhuma depende de outro chunk
 * ja existir. E por isso que uma ilha de 80.000 x 70.000 e viavel: o custo e
 * proporcional ao que o jogador visita, e nao ao tamanho do mapa.
 *
 * <p><b>A GUARDA DE DIMENSAO E A PRIMEIRA LINHA, e ela nao e paranoia.</b> Uma
 * feature entra nos biomas por {@code biome_modifier}, e modificador de bioma
 * NAO conhece dimensao -- ele casa por tag. Sem esta guarda, as oito cidades
 * de Greed Island nasceriam tambem no Overworld, nas mesmas coordenadas, e o
 * relato seria "apareceu uma cidade estranha perto da minha base".
 */
public class ObrasDaIlha extends Feature<NoneFeatureConfiguration> {

    /** A faixa em que o aterro da cidade desce ate o terreno natural. */
    private static final int TALUDE = 10;

    public ObrasDaIlha(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> contexto) {
        WorldGenLevel nivel = contexto.level();
        if (!(nivel instanceof net.minecraft.server.level.WorldGenRegion regiao)
                || !GreedIslandRegion.dentro(regiao.getLevel().dimension())) {
            return false;
        }
        BlockPos origem = contexto.origin();
        // AS TRES PERGUNTAS CARAS SAO FEITAS UMA VEZ POR CHUNK, e nao por
        // bloco. Sem isto, um chunk de mato no meio do nada -- que e a maior
        // parte da ilha -- paga 256 varreduras de balde de estrada para
        // descobrir que nao ha nada ali.
        boolean temEstrada = TracadoDeEstradas.algumaEstradaPertoDoChunk(
                origem.getX() >> 4, origem.getZ() >> 4);
        boolean temCidade = RegistroDeCidades.todas().stream().anyMatch(c ->
                Math.abs(origem.getX() + 8 - c.ancora().x()) <= c.larguraX() / 2 + 16
                        && Math.abs(origem.getZ() + 8 - c.ancora().z())
                                <= c.larguraZ() / 2 + 16);
        boolean temLandmark = RegistroDeLandmarks.todos().stream().anyMatch(l ->
                Math.abs(origem.getX() + 8 - l.ancora().x()) <= 24
                        && Math.abs(origem.getZ() + 8 - l.ancora().z()) <= 24);
        if (!temEstrada && !temCidade && !temLandmark) {
            return false;
        }
        boolean mexeu = false;

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = origem.getX() + dx;
                int z = origem.getZ() + dz;
                mexeu |= coluna(nivel, x, z, temEstrada, temLandmark);
            }
        }
        return mexeu;
    }

    /**
     * Uma coluna: a cidade tem prioridade, depois a estrada, depois o landmark.
     *
     * <p>A ORDEM IMPORTA. Estrada desenhada por cima de rua deixaria uma faixa
     * de cascalho atravessando a praca central; landmark por cima de casa
     * deixaria uma torre dentro de uma padaria.
     */
    private boolean coluna(WorldGenLevel nivel, int x, int z,
            boolean temEstrada, boolean temLandmark) {
        var cidade = RegistroDeCidades.em(x, z);
        if (cidade.isPresent()) {
            boolean fez = construirCidade(nivel, cidade.get(), x, z);
            // O LANDMARK VEM DEPOIS DA CIDADE, E NAO EM VEZ DELA.
            //
            // O TESTE EM JOGO ACHOU ISTO: a arvore Shiso nunca nascia. Ela e
            // landmark E fica na ancora da cidade Shiso Tree, e a versao
            // anterior saia da coluna assim que reconhecia a cidade -- entao o
            // landmark que DA NOME ao lugar era o unico que nao podia existir.
            // O mesmo valia para o farol de Soufrabi e para o castelo de
            // Limeiro.
            //
            // Ele vem DEPOIS porque precisa do chao aterrado: plantado antes,
            // o aterro o cortaria pela base.
            return (temLandmark && construirLandmark(nivel, x, z)) || fez;
        }
        if (temEstrada
                && TracadoDeEstradas.distanciaAte(x, z)
                        <= TracadoDeEstradas.MEIA_LARGURA + 2) {
            return construirEstrada(nivel, x, z);
        }
        return temLandmark && construirLandmark(nivel, x, z);
    }

    // ------------------------------------------------------------------
    // CIDADE
    // ------------------------------------------------------------------

    private boolean construirCidade(WorldGenLevel nivel, DefinicaoDeCidade cidade,
            int x, int z) {
        PlantaDeCidade.Uso uso = PlantaDeCidade.usoEm(cidade, x, z);
        if (uso == PlantaDeCidade.Uso.FORA) {
            return false;
        }
        int cota = PlantaDeCidade.cotaDe(cidade);
        aterrar(nivel, x, z, cota, pisoDe(uso, cidade));

        if (uso == PlantaDeCidade.Uso.LOTE && ehCantoDeLote(x, z)) {
            erguerPredio(nivel, cidade, x, z, cota);
        }
        return true;
    }

    /**
     * Um predio por lote, e nao um por bloco.
     *
     * <p>Sem esta condicao, cada bloco de lote viraria uma torre de um por um
     * -- uma cidade de espinhos. O canto do quarteirao e o unico ponto em que
     * a construcao nasce, e ela cresce dali.
     */
    private static boolean ehCantoDeLote(int x, int z) {
        return Math.floorMod(x, 8) == 4 && Math.floorMod(z, 8) == 4;
    }

    private void erguerPredio(WorldGenLevel nivel, DefinicaoDeCidade cidade,
            int x, int z, int cota) {
        int altura = PlantaDeCidade.alturaDoPredio(cidade, x, z);
        BlockState parede = paredeDe(cidade);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean borda = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                for (int y = 1; y <= altura; y++) {
                    // SO AS PAREDES, e nao o volume cheio: um bloco macico de
                    // nove por nove por dez nao e um predio, e uma pedra.
                    if (!borda && y < altura) {
                        continue;
                    }
                    // A JANELA E UM VAO, e nao vidro: vidro em toda parede
                    // custa render e le como estufa.
                    boolean janela = borda && y % 3 == 2 && (dx + dz) % 2 == 0;
                    nivel.setBlock(new BlockPos(x + dx, cota + y, z + dz),
                            janela ? Blocks.AIR.defaultBlockState() : parede, 2);
                }
            }
        }
    }

    /**
     * O aterro: a cota da cidade vira chao, e o excesso e cortado.
     *
     * <p>Ele ENCHE quando o terreno esta baixo e LIMPA quando esta alto. Sem a
     * limpeza, uma cidade numa encosta ficaria meio enterrada; sem o
     * enchimento, ela ficaria pendurada.
     */
    private void aterrar(WorldGenLevel nivel, int x, int z, int cota, BlockState piso) {
        int natural = nivel.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
        for (int y = Math.min(natural, cota) - 1; y < cota; y++) {
            nivel.setBlock(new BlockPos(x, y, z), Blocks.DIRT.defaultBlockState(), 2);
        }
        nivel.setBlock(new BlockPos(x, cota, z), piso, 2);
        for (int y = cota + 1; y <= Math.max(natural, cota) + TALUDE; y++) {
            BlockPos acima = new BlockPos(x, y, z);
            if (!nivel.getBlockState(acima).isAir()) {
                nivel.setBlock(acima, Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    private static BlockState pisoDe(PlantaDeCidade.Uso uso, DefinicaoDeCidade cidade) {
        return switch (uso) {
            case PRACA -> Blocks.POLISHED_ANDESITE.defaultBlockState();
            case AVENIDA -> Blocks.STONE_BRICKS.defaultBlockState();
            case RUA -> Blocks.COBBLESTONE.defaultBlockState();
            case LOTE -> Blocks.GRAVEL.defaultBlockState();
            case ABERTO -> Blocks.GRASS_BLOCK.defaultBlockState();
            case FORA -> Blocks.GRASS_BLOCK.defaultBlockState();
        };
    }

    /**
     * A paleta por papel da cidade.
     *
     * <p>Ela e a secao 136 em forma minima: cada cidade precisa de identidade
     * arquitetonica inequivoca, e "nao medieval generico" e pedido tres vezes
     * no documento. Um bloco por papel nao e a paleta final -- e o que impede
     * as oito de sairem iguais enquanto a paleta final nao existe.
     */
    private static BlockState paredeDe(DefinicaoDeCidade cidade) {
        return switch (cidade.papel()) {
            case ENTRADA -> Blocks.OAK_PLANKS.defaultBlockState();
            case HUB_INICIAL -> Blocks.WHITE_TERRACOTTA.defaultBlockState();
            case HUB_COMERCIAL -> Blocks.SMOOTH_SANDSTONE.defaultBlockState();
            case CARTAS -> Blocks.POLISHED_DIORITE.defaultBlockState();
            case SOCIAL -> Blocks.BIRCH_PLANKS.defaultBlockState();
            case JOGO -> Blocks.RED_TERRACOTTA.defaultBlockState();
            case PORTO -> Blocks.SPRUCE_PLANKS.defaultBlockState();
            case CAPITAL -> Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        };
    }

    // ------------------------------------------------------------------
    // ESTRADA
    // ------------------------------------------------------------------

    private boolean construirEstrada(WorldGenLevel nivel, int x, int z) {
        int y = nivel.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
        BlockPos chao = new BlockPos(x, y, z);
        if (nivel.getBlockState(chao).isAir()) {
            return false;
        }
        boolean pista = TracadoDeEstradas.naEstrada(x, z);
        nivel.setBlock(chao, pista
                ? Blocks.DIRT_PATH.defaultBlockState()
                : Blocks.COARSE_DIRT.defaultBlockState(), 2);
        // A ESTRADA LIMPA O QUE ESTA EM CIMA. Sem isso, a grama alta e a arvore
        // que a decoracao anterior plantou continuam la, e a estrada fica
        // invisivel debaixo do mato.
        for (int acima = 1; acima <= 3; acima++) {
            BlockPos p = chao.above(acima);
            if (!nivel.getBlockState(p).isAir()) {
                nivel.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
            }
        }
        return true;
    }

    // ------------------------------------------------------------------
    // LANDMARK
    // ------------------------------------------------------------------

    private boolean construirLandmark(WorldGenLevel nivel, int x, int z) {
        for (Landmark l : RegistroDeLandmarks.todos()) {
            int dx = x - l.ancora().x();
            int dz = z - l.ancora().z();
            if (Math.abs(dx) > 8 || Math.abs(dz) > 8) {
                continue;
            }
            return marcar(nivel, l, x, z, dx, dz);
        }
        return false;
    }

    /**
     * A forma de cada tipo de landmark.
     *
     * <p>SAO FORMAS MINIMAS, e o javadoc diz isso em vez de deixar a pobreza
     * parecer entrega: uma torre de pedra nao e o Spell Card Hall. O que elas
     * garantem e que o landmark EXISTE no mundo, num lugar reconhecivel, e que
     * o sistema de descoberta tem o que descobrir. A arte entra depois, por
     * jigsaw, sem mover nenhuma ancora.
     */
    private boolean marcar(WorldGenLevel nivel, Landmark l, int x, int z, int dx, int dz) {
        // DENTRO DA CIDADE, A BASE E A COTA DELA. O heightmap ali responde
        // pelo terreno natural, que o aterro acabou de cobrir -- e o landmark
        // nasceria enterrado sob a propria praca.
        int base = RegistroDeCidades.em(x, z)
                .map(c -> PlantaDeCidade.cotaDe(c) + 1)
                .orElseGet(() -> nivel.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z));
        double d = Math.hypot(dx, dz);

        return switch (l.tipo()) {
            case ARVORE_SHISO, ARVORE_ANTIGA -> {
                if (d > 3) {
                    yield false;
                }
                int altura = l.tipo() == Landmark.Tipo.ARVORE_SHISO ? 26 : 16;
                for (int y = 0; y < altura; y++) {
                    nivel.setBlock(new BlockPos(x, base + y, z),
                            Blocks.DARK_OAK_LOG.defaultBlockState(), 2);
                }
                yield true;
            }
            case FAROL -> {
                if (d > 3) {
                    yield false;
                }
                for (int y = 0; y < 22; y++) {
                    nivel.setBlock(new BlockPos(x, base + y, z), y == 21
                            ? Blocks.GLOWSTONE.defaultBlockState()
                            : Blocks.WHITE_CONCRETE.defaultBlockState(), 2);
                }
                yield true;
            }
            case SITIO_DE_GAME_MASTER, ARENA -> {
                if (d < 5 || d > 7) {
                    yield false;
                }
                for (int y = 0; y < 6; y++) {
                    nivel.setBlock(new BlockPos(x, base + y, z),
                            Blocks.POLISHED_BLACKSTONE.defaultBlockState(), 2);
                }
                yield true;
            }
            case ROCHEDO -> {
                if (d > 5) {
                    yield false;
                }
                int altura = (int) Math.round(9 - d);
                for (int y = 0; y < altura; y++) {
                    nivel.setBlock(new BlockPos(x, base + y, z),
                            Blocks.ANDESITE.defaultBlockState(), 2);
                }
                yield true;
            }
            case SITIO_DE_CARTA, HABITAT_UNICO, PASSO, CACHOEIRA -> {
                if (d > 2) {
                    yield false;
                }
                nivel.setBlock(new BlockPos(x, base, z),
                        Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 2);
                yield true;
            }
            case ASSENTAMENTO, PONTE_GRANDE -> {
                if (d > 6) {
                    yield false;
                }
                nivel.setBlock(new BlockPos(x, base, z),
                        Blocks.COBBLESTONE.defaultBlockState(), 2);
                if (d < 2) {
                    for (int y = 1; y <= 4; y++) {
                        nivel.setBlock(new BlockPos(x, base + y, z),
                                Blocks.OAK_PLANKS.defaultBlockState(), 2);
                    }
                }
                yield true;
            }
        };
    }
}
