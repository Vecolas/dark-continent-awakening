package com.darkcontinent.nenfoundation.worldtree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.worldtree.generation.WorldTreeBranchNetwork;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O campo que escolhe quais folhas acendem.
 *
 * <p>Duas perguntas, e a segunda importa tanto quanto a primeira: sao 15% das
 * folhas, e elas estao AGRUPADAS. Quinze por cento espalhados bloco a bloco
 * seriam o defeito do confete de volta -- e, pior, milhoes de fontes de luz
 * isoladas, cada uma pagando a propria propagacao.
 */
class WorldTreeFoliageTextureTest {

    private static final int SEEDS = 20;

    private static long seedAt(int index) {
        return 1_000L + index * 7_919L;
    }

    private static WorldTreeFoliagePlan plan(long seed) {
        WorldTreeLayout layout = WorldTreeLayoutGenerator.generate(seed, 0, 0);
        return WorldTreeFoliagePlan.of(layout, WorldTreeBranchNetwork.secondaryAndTertiary(layout));
    }

    @Test
    @DisplayName("15% das folhas brilham -- o numero que foi pedido, medido")
    void quinzePorCentoDasFolhasBrilham() {
        // A AMOSTRA E O VOLUME REAL DAS PRATELEIRAS, e nao um cubo qualquer: a
        // copa nao e uniforme no espaco, e um cubo mediria a densidade do campo
        // em vez da fracao das FOLHAS. As duas coisas so coincidem por acaso.
        double soma = 0.0;
        for (int index = 0; index < SEEDS; index++) {
            long seed = seedAt(index);
            long folhas = 0;
            long luminosas = 0;
            int i = 0;
            for (WorldTreeFoliageShelf shelf : plan(seed).shelves()) {
                // Uma prateleira a cada treze: a suite inteira precisa rodar em
                // segundos. A amostragem nao e a fonte da dispersao entre seeds
                // -- essa e da propria copa, e esta dita na assercao.
                if ((i++ % 13) != 0) {
                    continue;
                }
                int passo = Math.max(1, (int) (shelf.radius() / 12));
                for (int x = (int) (shelf.centerX() - shelf.radius());
                        x <= shelf.centerX() + shelf.radius(); x += passo) {
                    for (int z = (int) (shelf.centerZ() - shelf.radius());
                            z <= shelf.centerZ() + shelf.radius(); z += passo) {
                        for (int y = (int) shelf.minY(); y <= (int) shelf.maxY(); y++) {
                            if (shelf.normalized(x, y, z) > 1.0) {
                                continue;
                            }
                            folhas++;
                            if (WorldTreeFoliageTexture.brilha(x, y, z, seed)) {
                                luminosas++;
                            }
                        }
                    }
                }
            }
            double fracao = (double) luminosas / folhas;
            // A FAIXA POR SEED E LARGA, e vale dizer por que em vez de deixar
            // parecer desleixo: o medido vai de 11,9% a 17,2% nas vinte seeds.
            // A dispersao e real -- um campo de periodo longo amostrado sobre um
            // volume que muda de forma e de altitude a cada seed nao devolve a
            // mesma fracao --, e apertar isto so produziria um portao que reprova
            // por causa da seed, e nao por causa de uma regressao. Quem trava a
            // calibracao e a MEDIA, logo abaixo.
            assertTrue(fracao >= 0.10 && fracao <= 0.20,
                    "seed " + seed + ": " + String.format("%.1f%%", fracao * 100)
                            + " das folhas brilham, fora da faixa que as vinte"
                            + " seeds produzem hoje (11,9% a 17,2%).");
            soma += fracao;
        }
        double media = soma / SEEDS;
        // MEDIDO: 0,1531. A faixa e de um ponto e meio para cada lado.
        assertTrue(media >= 0.138 && media <= 0.168,
                "media de " + String.format("%.1f%%", media * 100) + " das folhas."
                        + " Se alguem mexeu em LIMIAR_DE_BRILHO, a conta que"
                        + " justifica o numero tem de ser refeita junto.");
    }

    @Test
    @DisplayName("a folha luminosa vem em TUFO, e nao em chuvisco")
    void brilhoAgrupado() {
        // O ANTIDOTO DO CONFETE, e tambem o do custo de luz. Um sorteio por bloco
        // a 15% produz corridas de ~1,2 blocos: cada bloco luminoso sozinho no
        // meio de folha comum, e cada um pagando a propria propagacao de luz.
        // O campo daqui produz manchas.
        long seed = seedAt(0);
        long corridas = 0;
        long blocosEmCorrida = 0;
        int atual = 0;
        for (int y : new int[] {900, 1000, 1200}) {
            for (int z = -150; z <= 150; z += 5) {
                for (int x = -300; x <= 300; x++) {
                    if (WorldTreeFoliageTexture.brilha(x, y, z, seed)) {
                        atual++;
                    } else if (atual > 0) {
                        blocosEmCorrida += atual;
                        corridas++;
                        atual = 0;
                    }
                }
            }
        }
        double corridaMedia = (double) blocosEmCorrida / corridas;
        assertTrue(corridaMedia >= 4.0,
                "corrida media de " + String.format("%.1f", corridaMedia) + " blocos."
                        + " Abaixo de 4 isso deixa de ser mancha e vira chuvisco --"
                        + " e chuvisco de fonte de luz e o pior caso para a engine.");
    }

    @Test
    @DisplayName("mesma coordenada, mesma resposta -- senao a folha pisca entre geracoes")
    void deterministico() {
        for (int index = 0; index < SEEDS; index += 4) {
            long seed = seedAt(index);
            for (int x = -40; x <= 40; x += 7) {
                for (int y = 880; y <= 1300; y += 53) {
                    for (int z = -40; z <= 40; z += 11) {
                        assertEquals(WorldTreeFoliageTexture.brilha(x, y, z, seed),
                                WorldTreeFoliageTexture.brilha(x, y, z, seed));
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("o atalho por coluna e a funcao completa dao a MESMA resposta")
    void atalhoDeColunaNaoDiverge() {
        // O GERADOR USA O ATALHO, e os outros testes usam a funcao completa. Se
        // os dois divergissem, a copa em jogo seria diferente da copa que esta
        // suite mede -- que e o estado que o portao de custo ja viveu uma vez, e
        // que e pior do que nao ter portao.
        for (int index = 0; index < SEEDS; index += 3) {
            long seed = seedAt(index);
            for (int x = -60; x <= 60; x += 3) {
                for (int z = -60; z <= 60; z += 5) {
                    double termo = WorldTreeFoliageTexture.termoDaColuna(x, z, seed);
                    for (int y = 900; y <= 1000; y += 7) {
                        assertEquals(WorldTreeFoliageTexture.brilha(x, y, z, seed),
                                WorldTreeFoliageTexture.brilha(termo, x, y, z, seed),
                                "x=" + x + " y=" + y + " z=" + z);
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("o seno tabelado vale o seno -- ele decide bloco, nao desenha grafico")
    void tabelaDeSenoEFiel() {
        // O ERRO TOLERADO AQUI NAO E ESTETICO: ele e a margem que separa a
        // decisao de um bloco da decisao oposta perto do limiar. 1e-5 e cerca de
        // cem vezes o erro medido da tabela, e ainda assim ordens de grandeza
        // abaixo da variacao do campo entre blocos vizinhos.
        double pior = 0.0;
        for (double radianos = -400.0; radianos <= 400.0; radianos += 0.00037) {
            pior = Math.max(pior,
                    Math.abs(WorldTreeFoliageTexture.sin(radianos) - Math.sin(radianos)));
        }
        assertTrue(pior < 1.0e-5,
                "a tabela erra ate " + pior + " -- perto do limiar isso troca a"
                        + " decisao de um bloco.");
    }

    @Test
    @DisplayName("o seno tabelado nao tem emenda em x=0 -- coordenada negativa e metade da arvore")
    void tabelaSemEmendaNoZero() {
        // ALIMENTAR O PORTAO COM O DEFEITO: trocar `Math.floor` por um cast na
        // indexacao da tabela trunca em direcao a zero, e o angulo e negativo
        // metade do tempo. O erro resultante e pequeno em media e CONCENTRADO em
        // torno de zero -- uma emenda reta atravessando a arvore, do tipo que se
        // atribui a geracao por chunk.
        for (double radianos = -3.0; radianos <= 3.0; radianos += 0.0001) {
            assertTrue(Math.abs(WorldTreeFoliageTexture.sin(radianos)
                    - Math.sin(radianos)) < 1.0e-5, "emenda em " + radianos);
        }
    }
}
