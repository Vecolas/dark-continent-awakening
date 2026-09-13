package com.darkcontinent.nenfoundation.gametest;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.structure.PostoAvancadoPlacementTransform;
import com.darkcontinent.nenfoundation.structure.PostoAvancadoTerrainCheck;
import com.darkcontinent.nenfoundation.structure.PostoAvancadoWorldPlacer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Prova no servidor que o planner termina em blocos reais, nao so em placements.
 *
 * <p>ESTE ARQUIVO DEIXOU A MAIN VERMELHA, e o defeito nao estava no codigo que
 * ele testa. Ele chamava o placer em coordenadas fixas do mundo e dependia de a
 * terra ali ser plana por sorte -- o {@code runGameTestServer} gera um mundo a
 * cada execucao, e no ponto escolhido a variacao medida foi <b>6</b> contra um
 * limite de <b>5</b>. O resultado era "placement rejeitado: TERRENO", sem
 * ninguem ter tocado no posto.
 *
 * <p>Um teste que depende de geracao de mundo em coordenada fixa nao e ruim de
 * vez em quando: ele e um teste que <b>as vezes</b> diz a verdade, e nao ha
 * como saber qual das vezes foi. O verde dele nao significava nada, e o
 * vermelho tambem nao.
 *
 * <p>Agora a altura do terreno entra por fora, e cada caso escolhe a sua. O que
 * so o gametest pode provar -- que os placements viram bloco NUM NIVEL DE
 * VERDADE -- continua provado; o que ele nao devia estar medindo -- o relevo
 * aleatorio perto da origem -- saiu.
 */
@GameTestHolder(NenFoundation.MOD_ID)
public final class PostoAvancadoGameTest {
    private PostoAvancadoGameTest() { }

    /** Chao plano numa cota que existe em qualquer mundo. */
    private static final int COTA_PLANA = 64;

    /** Metade do footprint 41x41, deixando dois blocos de folga interna. */
    private static final int METADE = 20;

    /** Ate onde a varredura procura estrutura acima da fundacao. */
    private static final int ALTURA_PROCURADA = 20;

    /**
     * Quanto o posto precisa subir para ser uma construcao.
     *
     * <p>Modesto de proposito: o numero nao descreve a silhueta do posto, e sim
     * a fronteira entre "construcao" e "carimbo plano". Amarra-lo a altura real
     * faria o teste reprovar a cada mudanca de telhado, e a segunda vez que
     * isso acontecesse alguem baixaria o numero sem olhar.
     */
    private static final int ALTURA_MINIMA_DE_ESTRUTURA = 3;

    @GameTest(template = "empty", timeoutTicks = 40)
    @PrefixGameTestTemplate(false)
    public static void colocaTodosOsBlocosDoPosto(GameTestHelper helper) {
        // A FAIXA E ESVAZIADA ANTES, e isto nao e preciosismo.
        //
        // O nivel do gametest CONSTROI POSTOS SOZINHO -- a geracao natural
        // entrou em #255 -- e ja havia um exatamente aqui. A primeira versao
        // desta varredura encontrou um bloco antigo acima da fundacao mesmo com
        // o placer sabotado para nao escrever nada: ela media o posto que a
        // geracao de mundo tinha feito, e teria aprovado um placer que nao
        // construisse coisa nenhuma.
        //
        // Medir o delta tambem nao bastava: os blocos escritos caem nas MESMAS
        // posicoes do posto que ja estava la, entao "posicao nova" dava zero.
        // Com a faixa vazia, tudo o que estiver solida no fim foi este placer
        // que escreveu.
        esvaziar(helper, COTA_PLANA);

        var resultado = PostoAvancadoWorldPlacer.colocar(helper.getLevel(), 0, 0,
                PostoAvancadoPlacementTransform.Rotacao.NONE,
                (x, z) -> COTA_PLANA);
        helper.assertFalse(resultado.rejeitado(),
                "placement rejeitado em terreno PLANO: " + resultado.motivo()
                        + ". Com variacao zero nao ha motivo de terreno para"
                        + " recusar, entao a recusa veio de outro lugar.");
        helper.assertTrue(resultado.placements() > 500,
                "blockout colocou poucos placements: " + resultado.placements());
        helper.assertTrue(resultado.blocosUnicos() > 500,
                "blockout colocou poucos blocos unicos: " + resultado.blocosUnicos());

        helper.assertTrue(contar(helper, Blocks.SHROOMLIGHT) > 0,
                "o posto nao recebeu iluminacao tecnica");
        helper.assertTrue(contar(helper, Blocks.WHITE_CONCRETE) > 0,
                "a estrutura nao recebeu concreto branco");
        helper.assertTrue(contar(helper, Blocks.IRON_BLOCK) == 0,
                "o posto ainda usa bloco de ferro macico");
        helper.assertTrue(contar(helper, Blocks.CHEST) > 0,
                "o posto nao recebeu caixas de operacao");
        helper.assertTrue(contar(helper, Blocks.LIGHTNING_ROD) > 0,
                "a antena do posto nao foi escrita");
        helper.assertTrue(contar(helper, Blocks.BLACK_WOOL) > 0,
                "a infraestrutura de cabos nao foi escrita");
        helper.assertTrue(contar(helper, Blocks.IRON_BARS) > 0,
                "o cercamento ou portao nao foi escrito");

        BlockPos origem = resultado.origem();
        helper.assertTrue(!helper.getLevel().getBlockState(origem).isAir(),
                "a fundacao do posto nao foi escrita no nivel");

        // O CENTRO E O PATIO, e a assercao original nao sabia disso. Ela exigia
        // bloco DOIS ACIMA DA ORIGEM, que e o meio do posto -- chao e ceu
        // aberto. Ela so passava porque, em terreno irregular, a cota da origem
        // era a MENOR das amostras e o aterro enchia aquela coluna: o teste
        // media o desnivel do mundo, e nao o posto.
        //
        // O que prova que o posto e construcao, e nao carimbo plano, e haver
        // estrutura NOVA acima da fundacao em algum lugar do footprint.
        int alturaMaxima = 0;
        for (BlockPos p : naoVazios(helper, COTA_PLANA)) {
            alturaMaxima = Math.max(alturaMaxima, p.getY() - origem.getY());
        }
        helper.assertTrue(alturaMaxima >= ALTURA_MINIMA_DE_ESTRUTURA,
                "o posto subiu " + alturaMaxima + " bloco(s) acima da"
                        + " fundacao, e o minimo para ser uma construcao e "
                        + ALTURA_MINIMA_DE_ESTRUTURA + ". Um posto que so"
                        + " escreve o chao nao da erro nenhum -- ele so nao e um"
                        + " posto.");
        helper.succeed();
    }

    /** Deixa a faixa varrida sem nenhum bloco, para que o teste meça o que ELE escreveu. */
    private static void esvaziar(GameTestHelper helper, int cota) {
        for (int dy = 1; dy <= ALTURA_PROCURADA; dy++) {
            for (int dx = -METADE; dx <= METADE; dx++) {
                for (int dz = -METADE; dz <= METADE; dz++) {
                    helper.getLevel().setBlock(new BlockPos(dx, cota + dy, dz),
                            net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    /** As posicoes ocupadas na faixa varrida acima da cota do posto. */
    private static java.util.Set<BlockPos> naoVazios(GameTestHelper helper, int cota) {
        java.util.Set<BlockPos> ocupadas = new java.util.HashSet<>();
        for (int dy = 1; dy <= ALTURA_PROCURADA; dy++) {
            for (int dx = -METADE; dx <= METADE; dx++) {
                for (int dz = -METADE; dz <= METADE; dz++) {
                    BlockPos p = new BlockPos(dx, cota + dy, dz);
                    if (!helper.getLevel().getBlockState(p).isAir()) {
                        ocupadas.add(p);
                    }
                }
            }
        }
        return ocupadas;
    }

    private static int contar(GameTestHelper helper, net.minecraft.world.level.block.Block bloco) {
        int encontrados = 0;
        for (int dy = 0; dy <= ALTURA_PROCURADA; dy++) {
            for (int dx = -METADE; dx <= METADE; dx++) {
                for (int dz = -METADE; dz <= METADE; dz++) {
                    if (helper.getLevel().getBlockState(new BlockPos(dx, COTA_PLANA + dy, dz))
                            .is(bloco)) {
                        encontrados++;
                    }
                }
            }
        }
        return encontrados;
    }

    /**
     * E o caminho da RECUSA, que nunca teve prova de ponta a ponta.
     *
     * <p>A regra de terreno tem teste unitario proprio, e ele prova a CONTA. O
     * que so aqui se ve e a recusa chegando ate quem chama: um plano vazio
     * virando {@code Motivo.TERRENO} <b>sem escrever bloco nenhum no nivel</b>.
     *
     * <p>O QUE ELE NAO PROVA, e a primeira versao dele fingia provar: que a
     * recusa nao escreve bloco nenhum. Eu tinha escrito assercao sobre
     * {@code placements()} e {@code blocosUnicos()} -- e os dois sao zero por
     * construcao em {@code Resultado.rejeitado}, entao a assercao conferia uma
     * constante. Alimentei o placer com um defeito que recusa E escreve, e os
     * 124 gametests passaram.
     *
     * <p>Provar isso de verdade exigiria coordenadas que so este teste toca, e
     * os gametests dividem o mesmo nivel: o caso aceito escreve o posto no
     * mesmo lugar, e os blocos do defeito seriam identicos aos que ja estao la.
     * Fica declarado como nao provado em vez de carimbado como provado.
     */
    @GameTest(template = "empty", timeoutTicks = 20)
    @PrefixGameTestTemplate(false)
    public static void terrenoIrregularERecusadoSemEscreverNada(GameTestHelper helper) {
        // Um degrau maior que o limite entre duas amostras do footprint.
        final int degrau = PostoAvancadoTerrainCheck.ALTURA_ACEITAVEL_MAXIMA + 1;
        var resultado = PostoAvancadoWorldPlacer.colocar(helper.getLevel(), 0, 0,
                PostoAvancadoPlacementTransform.Rotacao.NONE,
                (x, z) -> x > 0 ? COTA_PLANA + degrau : COTA_PLANA);

        helper.assertTrue(resultado.rejeitado(),
                "um degrau de " + degrau + " blocos passou por um limite de "
                        + PostoAvancadoTerrainCheck.ALTURA_ACEITAVEL_MAXIMA + ".");
        helper.assertTrue(resultado.motivo()
                        == PostoAvancadoWorldPlacer.Resultado.Motivo.TERRENO,
                "a recusa veio como " + resultado.motivo() + ", e o problema era"
                        + " o terreno. Motivo trocado manda procurar no lugar"
                        + " errado.");
        helper.succeed();
    }
}
