package com.darkcontinent.nenfoundation.client.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.client.vfx.AuraTransitionProfile.Componente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A linha do tempo da transicao, componente por componente.
 *
 * <p><b>O QUE ESTE ARQUIVO PROTEGE.</b> A diferenca entre "Ren parece liberacao"
 * e "Ren parece interpolacao" e inteira aqui, e ela e invisivel em revisao de
 * codigo: as duas versoes compilam, as duas desenham, e so uma delas tem a
 * contracao de 100 ms. Sem estes testes, a unica regua seria olhar um video
 * quadro a quadro -- e opiniao sobre video nao reprova nada.
 *
 * <p>O outro modo de falha que ele fecha e mais silencioso ainda: um componente
 * que nunca chega ao alvo. Uma janela que termine em 0,98 deixa a pressao de
 * chao 2% mais fraca PARA SEMPRE, e ninguem liga um anel levemente apagado a uma
 * tabela de fases meses depois.
 */
class AuraTransitionProfileTest {

    /** Quantos passos por transicao o teste amostra. Vinte ticks e um segundo. */
    private static final int PASSOS = 200;

    @Test
    @DisplayName("toda transicao TERMINA exatamente no alvo, componente por componente")
    void terminaNoAlvo() {
        for (AuraTransicao t : AuraTransicao.values()) {
            AuraTransitionSample fim = t.amostrar(1.0F);
            AuraTransitionSample esperado = alvoDe(t);
            assertEquals(esperado.shell(), fim.shell(), 1.0e-5F,
                    t + ": a shell nao fecha no alvo");
            assertEquals(esperado.borda(), fim.borda(), 1.0e-5F,
                    t + ": a borda nao fecha no alvo -- ela ficaria acima ou abaixo"
                            + " PARA SEMPRE, e ninguem liga isso a uma curva de easing");
            assertEquals(esperado.filamentos(), fim.filamentos(), 1.0e-5F, t.toString());
            assertEquals(esperado.colunas(), fim.colunas(), 1.0e-5F, t.toString());
            assertEquals(esperado.pressao(), fim.pressao(), 1.0e-5F, t.toString());
            assertEquals(0.0F, fim.flash(), 1.0e-5F,
                    t + ": o flash e um GESTO, e gesto que nao termina e um estado");
        }
    }

    /** O estado assentado que cada transicao deveria produzir no fim. */
    private static AuraTransitionSample alvoDe(AuraTransicao t) {
        return switch (t) {
            case LIGAR -> AuraTransitionSample.SEM_PRESSAO;
            case ELEVAR -> AuraTransitionSample.CHEIO;
            // BAIXAR vai para Ten: coluna e pressao zeram, o resto fica.
            case BAIXAR -> AuraTransitionSample.SEM_PRESSAO;
            case SUPRIMIR, DESLIGAR -> AuraTransitionSample.VAZIO;
            case PADRAO -> AuraTransitionSample.CHEIO;
        };
    }

    @Test
    @DisplayName("nenhum peso sai da faixa, em nenhum instante de nenhuma transicao")
    void nadaSaiDaFaixa() {
        for (AuraTransicao t : AuraTransicao.values()) {
            for (int i = 0; i <= PASSOS; i++) {
                float p = i / (float) PASSOS;
                // O PROPRIO CONSTRUTOR DE AuraTransitionSample RECUSA valor fora
                // de faixa. Amostrar a transicao inteira e o que transforma essa
                // recusa numa garantia sobre a TABELA, e nao apenas sobre o
                // record.
                AuraTransitionSample s = t.amostrar(p);
                assertTrue(s.shell() >= 0.0F && s.shell() <= 1.0F,
                        t + " em " + p + ": shell fora de 0..1 -- so a borda pode"
                                + " ultrapassar");
                assertTrue(s.borda() <= AuraTransitionSample.TETO_DE_ULTRAPASSAGEM,
                        t + " em " + p + ": borda acima do teto de ultrapassagem");
            }
        }
    }

    // ------------------------------------------------- TEN -> REN

    @Test
    @DisplayName("a shell CONTRAI nos primeiros 100 ms, e volta depois")
    void contracaoAcontece() {
        AuraTransitionProfile elevar = AuraTransicao.ELEVAR.fases();

        float noComeco = elevar.valorDe(Componente.SHELL, 0.0F);
        float noFundo = elevar.valorDe(Componente.SHELL, 100.0F);
        float depois = elevar.valorDe(Componente.SHELL, 500.0F);

        assertEquals(1.0F, noComeco, 1.0e-5F, "a subida parte de Ten inteiro");
        assertTrue(noFundo < noComeco,
                "SEM CONTRACAO A SUBIDA E UMA RAMPA, e rampa e lida como interpolacao."
                        + " Este e o detalhe que faz Ren parecer liberacao: " + noFundo);
        float contracao = 1.0F - noFundo;
        assertTrue(contracao >= 0.03F && contracao <= 0.05F,
                "a direcao de arte pede 3 a 5% de contracao; veio " + (contracao * 100.0F) + "%");
        assertEquals(1.0F, depois, 1.0e-5F, "a shell precisa VOLTAR ao cheio");
    }

    @Test
    @DisplayName("a borda ultrapassa entre 10 e 15%, e assenta exatamente em 1")
    void ultrapassagemDentroDaFaixa() {
        AuraTransitionProfile elevar = AuraTransicao.ELEVAR.fases();

        float pico = 0.0F;
        for (int ms = 0; ms <= 900; ms++) {
            pico = Math.max(pico, elevar.valorDe(Componente.BORDA, ms));
        }
        assertTrue(pico >= 1.10F && pico <= 1.15F,
                "a ultrapassagem precisa ficar entre 10 e 15%; veio " + pico);
        assertEquals(1.0F, elevar.valorDe(Componente.BORDA, 900.0F), 1.0e-5F,
                "UMA ULTRAPASSAGEM QUE NAO FECHA deixa a borda acima do alvo para sempre");
        assertEquals(1.0F, elevar.valorDe(Componente.BORDA, 0.0F), 1.0e-5F,
                "antes da janela, a borda e a de Ten -- e nao zero");
    }

    @Test
    @DisplayName("as colunas entram ANTES da pressao de chao, e as duas so em Ren")
    void ordemDasFases() {
        AuraTransitionProfile elevar = AuraTransicao.ELEVAR.fases();

        assertEquals(0.0F, elevar.valorDe(Componente.COLUNAS, 300.0F), 1.0e-5F,
                "as colunas comecam em 350 ms");
        assertTrue(elevar.valorDe(Componente.COLUNAS, 500.0F) > 0.0F,
                "as colunas ja estao entrando na metade da subida");
        assertEquals(0.0F, elevar.valorDe(Componente.PRESSAO, 450.0F), 1.0e-5F,
                "A PRESSAO E A ULTIMA A ENTRAR. Invertida, o chao reagiria antes de o"
                        + " corpo ter mudado -- e o olho le isso como causa e efeito"
                        + " trocados");
        assertTrue(elevar.valorDe(Componente.COLUNAS, 600.0F)
                        > elevar.valorDe(Componente.PRESSAO, 600.0F),
                "em qualquer instante da subida, a coluna esta mais adiante que o chao");
    }

    @Test
    @DisplayName("cada fase e monotona: nada sobe, desce e sobe de novo dentro da janela")
    void monotonicidadePorFase() {
        for (AuraTransicao t : AuraTransicao.values()) {
            for (Componente c : Componente.values()) {
                // O FLASH E A EXCECAO DECLARADA: ele sobe e desce de proposito,
                // em DUAS janelas, e cada uma delas e monotona. Conferir o
                // conjunto como se fosse uma so reprovaria o gesto certo.
                if (c == Componente.FLASH) {
                    continue;
                }
                verificarMonotonia(t, c);
            }
        }
    }

    /**
     * Confere que o componente nao inverte o sentido MAIS DE UMA VEZ.
     *
     * <p>UMA INVERSAO E LEGITIMA -- e a contracao da shell, que desce e sobe. O
     * que reprova e a terceira mudanca de sentido: isso e oscilacao, e oscilacao
     * dentro de uma transicao le como tremor.
     */
    private static void verificarMonotonia(AuraTransicao t, Componente c) {
        AuraTransitionProfile perfil = t.fases();
        float anterior = perfil.valorDe(c, 0.0F);
        int inversoes = 0;
        int sentido = 0;
        for (int i = 1; i <= PASSOS; i++) {
            float ms = perfil.duracaoMs() * i / PASSOS;
            float atual = perfil.valorDe(c, ms);
            float delta = atual - anterior;
            if (Math.abs(delta) > 1.0e-6F) {
                int novo = delta > 0.0F ? 1 : -1;
                if (sentido != 0 && novo != sentido) {
                    inversoes++;
                }
                sentido = novo;
            }
            anterior = atual;
        }
        assertTrue(inversoes <= 1,
                t + "/" + c + " inverte o sentido " + inversoes + " vezes; mais de uma"
                        + " inversao le como tremor dentro da transicao");
    }

    // ------------------------------------------------- QUALQUER -> ZETSU

    @Test
    @DisplayName("a supressao chega a ZERO literal -- e nao a 0.01")
    void supressaoChegaAoZero() {
        AuraTransitionProfile fechar = AuraTransicao.SUPRIMIR.fases();
        float fim = fechar.duracaoMs();

        for (Componente c : Componente.values()) {
            assertEquals(0.0F, fechar.valorDe(c, fim), 0.0F,
                    "ZERO LITERAL, e nao aproximado: " + c + " sobrou em "
                            + fechar.valorDe(c, fim) + ". Num servidor com dois clientes,"
                            + " brilho residual entrega justamente quem esta se escondendo");
        }
    }

    @Test
    @DisplayName("a supressao fecha em fases: primeiro os filamentos, a borda por ultimo")
    void supressaoEmFases() {
        AuraTransitionProfile fechar = AuraTransicao.SUPRIMIR.fases();
        float d = fechar.duracaoMs();

        // Em 1/4 da duracao os filamentos ja sairam e a borda esta intacta.
        assertEquals(0.0F, fechar.valorDe(Componente.FILAMENTOS, d * 0.25F), 1.0e-5F);
        assertEquals(1.0F, fechar.valorDe(Componente.BORDA, d * 0.25F), 1.0e-5F,
                "A BORDA E A ULTIMA A SAIR, como no LOD: e ela que carrega a leitura");
        // Na metade ainda ha shell, e a borda continua cheia.
        assertTrue(fechar.valorDe(Componente.SHELL, d * 0.45F) > 0.0F,
                "na metade do fechamento ainda ha pelicula -- senao nao sao fases,"
                        + " e uma rampa");
    }

    @Test
    @DisplayName("suprimir dura entre 200 e 500 ms, como a direcao de arte pede")
    void duracaoDaSupressao() {
        float d = AuraTransicao.SUPRIMIR.fases().duracaoMs();
        assertTrue(d >= 200.0F && d <= 500.0F, "supressao em " + d + " ms");
    }

    // ------------------------------------------------- estrutura

    @Test
    @DisplayName("uma linha do tempo sem todos os componentes e RECUSADA")
    void componenteEsquecidoReprova() {
        // UM COMPONENTE OMITIDO DESENHA ZERO, e zero por esquecimento e
        // indistinguivel de zero de proposito. E por isso que `montar` exige os
        // seis -- e por isso que este teste existe: ele e o caso que DEVE
        // reprovar, alimentado de proposito.
        var construtor = AuraTransitionProfile.de(AuraVisualMode.TEN, AuraVisualMode.REN, 500.0F)
                .constante(Componente.SHELL, 1.0F)
                .constante(Componente.BORDA, 1.0F);
        assertThrows(IllegalStateException.class, construtor::montar);
    }

    @Test
    @DisplayName("janelas fora de ordem sao RECUSADAS na montagem")
    void janelasForaDeOrdemReprovam() {
        var construtor = AuraTransitionProfile.de(AuraVisualMode.TEN, AuraVisualMode.REN, 500.0F);
        assertThrows(IllegalArgumentException.class, () -> construtor.fase(Componente.SHELL,
                new AuraTransitionProfile.Janela(200.0F, 400.0F, 0.0F, 1.0F,
                        AuraTransicao.Curva.LINEAR),
                new AuraTransitionProfile.Janela(100.0F, 300.0F, 1.0F, 0.0F,
                        AuraTransicao.Curva.LINEAR)));
    }

    @Test
    @DisplayName("uma janela que termina depois do fim da transicao e RECUSADA")
    void janelaAlemDoFimReprova() {
        var construtor = AuraTransitionProfile.de(AuraVisualMode.TEN, AuraVisualMode.REN, 300.0F);
        assertThrows(IllegalArgumentException.class, () -> construtor.fase(Componente.PRESSAO,
                new AuraTransitionProfile.Janela(100.0F, 400.0F, 0.0F, 1.0F,
                        AuraTransicao.Curva.LINEAR)));
    }

    @Test
    @DisplayName("o buraco entre duas janelas nao vira um pisco para zero")
    void buracoEntreJanelasSegura() {
        AuraTransitionProfile elevar = AuraTransicao.ELEVAR.fases();
        // Entre 500 (fim da recuperacao da shell) e 900 (fim da transicao) nao
        // ha janela de SHELL. O valor precisa FICAR em 1, e nao cair a zero.
        for (int ms = 500; ms <= 900; ms += 25) {
            assertEquals(1.0F, elevar.valorDe(Componente.SHELL, ms), 1.0e-5F,
                    "a shell piscou em " + ms + " ms: fora da janela o valor e o FIM da"
                            + " anterior, e nunca zero");
        }
    }

    @Test
    @DisplayName("o estado assentado de cada modo e o que a direcao de arte pede")
    void assentadoPorModo() {
        assertEquals(AuraTransitionSample.VAZIO,
                AuraTransitionSample.assentado(AuraVisualMode.ZETSU),
                "Zetsu e ausencia TOTAL");
        assertEquals(AuraTransitionSample.VAZIO,
                AuraTransitionSample.assentado(AuraVisualMode.OFF));
        assertEquals(0.0F, AuraTransitionSample.assentado(AuraVisualMode.TEN).pressao(),
                "TEN NAO TOCA O CHAO");
        assertEquals(0.0F, AuraTransitionSample.assentado(AuraVisualMode.TEN).colunas(),
                "coluna vertical e de Ren, e nao de Ten");
        assertEquals(1.0F, AuraTransitionSample.assentado(AuraVisualMode.REN).pressao());
        assertEquals(1.0F, AuraTransitionSample.assentado(AuraVisualMode.REN).colunas());
    }

    @Test
    @DisplayName("a linha do tempo estica com a duracao, sem mudar de forma")
    void escalaPreservaAForma() {
        // O jogador pode dobrar a duracao das transicoes na config. As FASES
        // precisam esticar junto: se elas ficassem em milissegundos absolutos
        // contra uma duracao maior, a pressao de chao entraria na metade do
        // caminho em vez de no fim.
        AuraTransitionProfile curto = AuraTransitionProfile
                .de(AuraVisualMode.TEN, AuraVisualMode.ZETSU, 300.0F)
                .fase(Componente.SHELL, AuraTransitionProfile
                        .de(AuraVisualMode.TEN, AuraVisualMode.ZETSU, 300.0F)
                        .fracao(0.25F, 0.625F, 1.0F, 0.0F, AuraTransicao.Curva.LINEAR))
                .constante(Componente.BORDA, 1.0F)
                .constante(Componente.FILAMENTOS, 1.0F)
                .constante(Componente.COLUNAS, 0.0F)
                .constante(Componente.PRESSAO, 0.0F)
                .constante(Componente.FLASH, 0.0F)
                .montar();

        // No mesmo PROGRESSO relativo, o valor e o mesmo -- e e isso que
        // significa "a forma nao mudou".
        assertEquals(1.0F, curto.amostrar(0.25F).shell(), 1.0e-5F);
        assertEquals(0.0F, curto.amostrar(0.625F).shell(), 1.0e-5F);
    }
}
