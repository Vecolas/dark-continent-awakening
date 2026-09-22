package com.darkcontinent.nenfoundation.client.vfx;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import com.darkcontinent.nenfoundation.api.SinalDeAura;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao do estado preso na MORTE e na TROCA DE DIMENSAO (AV0, #169).
 *
 * <p>POR QUE ELE EXISTE. O criterio de aceite do AV0 pede que {@code /nenvfx
 * off}, relog, morte e troca de dimensao nao deixem estado preso. O relog tinha
 * teste ({@link SobreposicaoDeVfxTest}) e um ponto de saida proprio --
 * {@code ClientPlayerNetworkEvent.LoggingOut}, com dezesseis limpezas. <b>Morte
 * e troca de dimensao nao tinham nem uma coisa nem outra</b>, e a unica prova
 * que existia delas era uma sessao humana de 2026-09-22.
 *
 * <p><b>E elas nao tem ponto de saida PROPOSITALMENTE.</b> Na morte e na troca
 * de dimensao o cliente nao faz logout: ele TROCA a entidade do jogador, que
 * volta com um <b>id novo</b>. Quem guarda estado por id de entidade nao e
 * avisado de nada. O que impede o vazamento e outra coisa --
 * {@link DetectorDeAtivacaoDeTen#reterSomente(Set)}, chamado a cada tick com
 * quem esta presente AGORA. O id velho simplesmente deixa de estar na lista.
 *
 * <p>Esse desenho e melhor que um handler de morte, e o motivo esta no proprio
 * {@code DetectorDeAtivacaoDeTen}: <i>"bastaria um reterSomente esquecido num
 * deles para as bordas de Ren continuarem sendo detectadas para alguem que ja
 * saiu do alcance -- e o sintoma seria um zumbido tocando sem dono"</i>. Um
 * handler por evento cobre os eventos que alguem lembrou; a poda por presenca
 * cobre todos, inclusive os que ninguem previu.
 *
 * <p><b>O QUE DA ERRADO SEM ISTO, e nao da erro:</b> o jogador morre em Ren, o
 * id antigo fica no mapa com {@code REN} guardado, e o id novo entra do zero.
 * Nada trava, nada lanca -- o mapa so cresce a cada morte, e o zumbido de um
 * jogador que ja nao existe continua sendo considerado ligado. E o mesmo padrao
 * do erro nº 8 do {@code CLAUDE.md}: nao aparece como erro, aparece como
 * consumo subindo devagar.
 *
 * <p><b>PONTO CEGO DECLARADO.</b> O teste de mecanismo abaixo prova a PODA;
 * {@link #aPodaEstaLigadaNoTickDoAudio()} prova que ela esta CHAMADA no tick --
 * mas por leitura de texto, e so em {@code AudioDeAura}. Estado por id de
 * entidade que nasca em outro arquivo, ou poda que passe a ser chamada por um
 * caminho que a regua nao reconhece, continua invisivel. E nada aqui sobe um
 * cliente: a prova de que a aura some na tela ao morrer continua sendo humana.
 */
class MorteETrocaDeDimensaoTest {

    /** Ids arbitrarios: o que importa e que o de depois seja DIFERENTE do de antes. */
    private static final int ANTES = 42;
    private static final int DEPOIS = 4711;
    private static final int OUTRO_JOGADOR = 7;

    @Test
    @DisplayName("morrer em Ren nao deixa o id antigo no mapa, e o novo comeca limpo")
    void morrerEmRenNaoDeixaEstadoPreso() {
        DetectorDeAtivacaoDeTen detector = new DetectorDeAtivacaoDeTen();

        // Vivo e em Ren, observado por dois ticks: a base e REN.
        detector.registrar(ANTES, SinalDeAura.REN);
        SinalDeAura base = detector.registrar(ANTES, SinalDeAura.REN);
        assertTrue(DetectorDeAtivacaoDeTen.liberada(base),
                "A base do teste esta errada: o jogador deveria estar em aura liberada antes de"
                        + " morrer, senao o cenario nao mede nada.");

        // Morreu. A entidade volta com OUTRO id, e o tick seguinte so ve o novo.
        detector.reterSomente(Set.of(DEPOIS));

        // O id velho foi esquecido: registrar de novo devolve null, e nao REN.
        assertNull(detector.registrar(ANTES, SinalDeAura.NENHUM),
                "O id anterior a morte continuou no mapa. Ele nunca mais sera observado -- a"
                        + " entidade nao existe --, entao a entrada fica para sempre, e o mapa"
                        + " cresce uma linha por morte. Nao da erro; da memoria subindo devagar.");
    }

    @Test
    @DisplayName("renascer nao toca ativacao fantasma -- a primeira observacao e base, nao borda")
    void renascerNaoInventaAtivacao() {
        DetectorDeAtivacaoDeTen detector = new DetectorDeAtivacaoDeTen();
        detector.registrar(ANTES, SinalDeAura.REN);
        detector.reterSomente(Set.of(DEPOIS));

        // O jogador renasce JA com Ten ligado (o servidor reenvia o estado).
        assertFalse(detector.atualizar(DEPOIS, SinalDeAura.TEN),
                "Renascer tocou o som de ATIVACAO de Ten. A primeira observacao de um id novo"
                        + " estabelece a base e nao e borda -- senao toda morte soaria como uma"
                        + " ativacao que o jogador nunca fez.");

        assertFalse(DetectorDeAtivacaoDeTen.entrouEmLiberacao(null, SinalDeAura.REN),
                "Renascer JA em Ren disparou o estouro de aura liberada. Sem sinal anterior nao"
                        + " ha borda: o som de Ren tem de vir de uma transicao observada, e nao"
                        + " do primeiro quadro em que o jogador novo aparece.");
    }

    @Test
    @DisplayName("trocar de dimensao e o mesmo caso -- e o mapa nao cresce")
    void trocarDeDimensaoNaoAcumula() {
        DetectorDeAtivacaoDeTen detector = new DetectorDeAtivacaoDeTen();

        // Overworld: o jogador local e mais alguem por perto.
        detector.registrar(ANTES, SinalDeAura.REN);
        detector.registrar(OUTRO_JOGADOR, SinalDeAura.TEN);

        // Trocou de dimensao: id novo, e o vizinho ficou para tras.
        detector.reterSomente(Set.of(DEPOIS));

        assertNull(detector.registrar(ANTES, SinalDeAura.NENHUM),
                "O id de antes da troca de dimensao sobreviveu.");
        assertNull(detector.registrar(OUTRO_JOGADOR, SinalDeAura.NENHUM),
                "O vizinho da dimensao anterior sobreviveu. Ele nao esta mais na tela, e o"
                        + " zumbido dele continuaria sendo considerado ligado -- som sem dono.");
    }

    @Test
    @DisplayName("SEM a poda o estado FICA preso -- prova de que e ela que trabalha")
    void semAPodaOEstadoFicaPreso() {
        DetectorDeAtivacaoDeTen detector = new DetectorDeAtivacaoDeTen();
        detector.registrar(ANTES, SinalDeAura.REN);

        // Exatamente o cenario dos testes acima, MENOS a chamada de reterSomente.
        SinalDeAura sobrevivente = detector.registrar(ANTES, SinalDeAura.NENHUM);

        assertTrue(sobrevivente == SinalDeAura.REN,
                "Sem reterSomente o id antigo DEVERIA permanecer. Se esta asercao falhou, o"
                        + " detector ganhou outra forma de esquecer e os tres testes acima podem"
                        + " estar passando por um motivo diferente do que afirmam medir.");
    }

    @Test
    @DisplayName("a poda esta LIGADA no tick do audio -- mecanismo correto nao basta se ninguem chama")
    void aPodaEstaLigadaNoTickDoAudio() {
        String fonte = Repo.texto(
                "src/main/java/com/darkcontinent/nenfoundation/client/vfx/AudioDeAura.java");

        assertTrue(Pattern.compile("\\breterSomente\\s*\\(").matcher(fonte).find(),
                "AudioDeAura parou de chamar reterSomente. Os outros testes deste arquivo"
                        + " continuariam VERDES -- eles provam que a poda funciona, nao que ela"
                        + " roda. Sem a chamada, cada morte e cada troca de dimensao deixam uma"
                        + " entrada morta no mapa, e o sintoma e consumo subindo devagar ao longo"
                        + " de uma sessao, que ninguem liga a este arquivo.");
    }
}
