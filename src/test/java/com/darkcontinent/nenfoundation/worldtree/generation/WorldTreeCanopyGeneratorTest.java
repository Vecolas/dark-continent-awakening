package com.darkcontinent.nenfoundation.worldtree.generation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O ESCRITOR da copa. A geometria dela e provada em
 * {@code WorldTreeFoliagePlanTest}; o que sobra aqui e a traducao para bloco.
 *
 * <p>Este arquivo mudou de assunto junto com o gerador. Ele testava
 * {@code clusterRadius}, um numero que so existia dentro do laco que escrevia
 * blocos -- e por isso a forma da copa nunca teve regua. O plano agora e puro, e
 * as perguntas daqui sao duas: o miolo da massa NAO e perfurado, e os atalhos de
 * custo do laco interno dao a MESMA resposta que a funcao completa. O corte por
 * chunk mudou de casa e tem portao proprio em {@code WorldTreeFoliageIndexTest}.
 */
class WorldTreeCanopyGeneratorTest {

    // O CORTE POR CHUNK MUDOU DE CASA, e o teste dele foi junto.
    //
    // Ele era um `if` de caixa dentro de um laco sobre o plano inteiro; hoje e
    // WorldTreeFoliageIndex, e a prova de que o resultado nao mudou esta em
    // WorldTreeFoliageIndexTest#indiceNaoMudaResultado, que compara o indice com
    // o laco linear em vinte seeds. Deixar uma copia do corte aqui seria manter
    // duas fontes para a mesma verdade -- e a copia morta divergiria da viva sem
    // nada acusar.

    @Test
    @DisplayName("o termo de coluna e a funcao completa concordam -- senao a casca muda em jogo")
    void cascaNaColunaConcordaComKeep() {
        // O GERADOR USA O ATALHO POR COLUNA; esta suite usa a funcao completa. Se
        // os dois divergissem, a borda da folhagem em jogo seria diferente da que
        // os outros testes daqui medem -- e os testes continuariam verdes.
        for (long seed : new long[] {42L, 1_000L, 8_919L}) {
            for (int x = -50; x <= 50; x += 3) {
                for (int z = -50; z <= 50; z += 5) {
                    double termo = WorldTreeCanopyGenerator.cascaNaColuna(x, z, seed);
                    for (int y = 890; y <= 950; y += 7) {
                        for (double normalized : new double[] {0.30, 0.75, 0.86, 0.97}) {
                            assertTrue(WorldTreeCanopyGenerator.keep(normalized, x, y, z, seed)
                                            == WorldTreeCanopyGenerator.keep(normalized, termo,
                                                    x, y, z, seed),
                                    "x=" + x + " y=" + y + " z=" + z + " n=" + normalized);
                        }
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("o MIOLO nunca e perfurado -- o ruido so opera na casca")
    void mioloSolido() {
        // ESTE E O ANTIDOTO DO CONFETE. A versao anterior sorteava bloco a bloco
        // no volume TODO e descartava 62% -- o que produz chuvisco, e nao
        // folhagem. Aqui, abaixo do inicio da casca a resposta e sim, sempre.
        for (int x = -30; x <= 30; x += 3) {
            for (int y = 890; y <= 910; y += 2) {
                assertTrue(WorldTreeCanopyGenerator.keep(0.30, x, y, 7, 42L),
                        "o miolo foi perfurado em x=" + x + " y=" + y);
                assertTrue(WorldTreeCanopyGenerator.keep(0.70, x, y, 7, 42L),
                        "furo logo abaixo do inicio da casca, em x=" + x + " y=" + y);
            }
        }
    }

    @Test
    @DisplayName("o miolo continua solido mesmo com a guarda de atalho removida")
    void oMioloNaoDependeDoAtalho() {
        // A PRIMEIRA VERSAO DESTA VERIFICACAO NAO PEGAVA A QUEBRA, e vale
        // registrar: apagar a guarda `normalized <= INICIO_DA_CASCA` nao mudava
        // resultado nenhum, porque abaixo do inicio da casca o termo `borda` fica
        // NEGATIVO e a comparacao com o ruido -- que nunca e negativo -- passa de
        // qualquer jeito. A guarda e um atalho de custo, e nao a semantica.
        //
        // O defeito de verdade era outro: ruido decidindo no volume INTEIRO, com
        // corte alto. Este teste mede a consequencia -- fracao do miolo que
        // sobrevive --, e nao a presenca de uma linha de codigo.
        int fica = 0;
        int total = 0;
        for (int x = -30; x <= 30; x++) {
            for (int z = -30; z <= 30; z++) {
                total++;
                if (WorldTreeCanopyGenerator.keep(0.40, x, 900, z, 42L)) {
                    fica++;
                }
            }
        }
        assertTrue(fica == total,
                "so " + fica + " de " + total + " blocos do miolo sobreviveram."
                        + " Miolo perfurado por ruido e CONFETE, e foi assim que a"
                        + " folhagem anterior se lia em jogo.");
    }

    @Test
    @DisplayName("o ruido da casca NAO apaga lajes inteiras de X")
    void ruidoNaoEProdutoSeparavel() {
        // ACHADO DE REVISAO, e o artefato era o pior possivel aqui.
        //
        // O ruido era `sin(x) * cos(z) * sin(y)` -- um produto SEPARAVEL. Quando
        // o primeiro fator passa por zero, a cada ~15 blocos em X, o produto
        // inteiro zera para TODO z e TODO y, e a borda da folhagem some numa LAJE
        // inteira. Em tela isso le como COSTURA DE CHUNK: quem visse iria
        // procurar o defeito na geracao por chunk, que esta certa.
        //
        // Este teste varre X e exige que nenhuma laje fique quase vazia.
        for (int x = -80; x <= 80; x++) {
            int fica = 0;
            int total = 0;
            for (int z = -20; z <= 20; z++) {
                for (int y = 890; y <= 910; y++) {
                    total++;
                    if (WorldTreeCanopyGenerator.keep(0.86, x, y, z, 42L)) {
                        fica++;
                    }
                }
            }
            double fracao = (double) fica / total;
            assertTrue(fracao > 0.15,
                    "a laje x=" + x + " ficou com " + String.format("%.0f%%", fracao * 100)
                            + " da casca. Uma laje apagada le como costura de chunk.");
        }
    }

    @Test
    @DisplayName("a casca esgarca: nem tudo fica, e nem tudo sai")
    void cascaIrregular() {
        int fica = 0;
        int total = 0;
        for (int x = -40; x <= 40; x++) {
            for (int z = -40; z <= 40; z++) {
                total++;
                if (WorldTreeCanopyGenerator.keep(0.97, x, 900, z, 42L)) {
                    fica++;
                }
            }
        }
        assertTrue(fica > 0, "a borda inteira sumiu: a prateleira fica com corte de faca");
        assertTrue(fica < total, "a borda inteira ficou: sem irregularidade, o disco fica de plastico");
    }
}
