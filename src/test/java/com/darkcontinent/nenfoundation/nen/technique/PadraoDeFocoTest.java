package com.darkcontinent.nenfoundation.nen.technique;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.nen.aura.AlocacaoDeAura;
import com.darkcontinent.nenfoundation.nen.aura.FocoDeAura;
import com.darkcontinent.nenfoundation.nen.aura.RegiaoDoCorpo;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O portao do padrao de foco, e ele nasceu de um defeito VISTO EM JOGO.
 *
 * <p>Ko concentrava na CABECA. A causa nao era o Ko: era um padrao unico em
 * {@code NenGyoService}, escolhido pensando em Gyo -- <i>"Gyo nos olhos e a
 * aplicacao famosa"</i> -- e herdado por toda tecnica que concentra.
 *
 * <p><b>E NAO ERA SO ESTETICO.</b> {@code NenDanoService} mede o reforco do
 * golpe pela alocacao no braco principal. Com a aura na cabeca, o jogador
 * pagava a tecnica mais cara do jogo, ficava com o corpo nu por um segundo, e o
 * soco saia sem o reforco. Nenhuma excecao, nenhum log.
 */
class PadraoDeFocoTest {

    private static final RegiaoDoCorpo DESTRO = RegiaoDoCorpo.BRACO_DIREITO;
    private static final RegiaoDoCorpo CANHOTO = RegiaoDoCorpo.BRACO_ESQUERDO;

    private static Gyo gyo() {
        return new Gyo(() -> 2.7D, () -> 0.45D);
    }

    private static Ko ko() {
        return new Ko(() -> 20.0D, () -> 0.95D, () -> 20, relogioMudo(), () -> 0.25D);
    }

    private static Ko.RelogioDeKo relogioMudo() {
        return new Ko.RelogioDeKo() {
            @Override public void iniciar(net.minecraft.server.level.ServerPlayer j, int t) { }
            @Override public boolean passarTickEVerSeAcabou(
                    net.minecraft.server.level.ServerPlayer j) {
                return false;
            }
            @Override public void limpar(net.minecraft.server.level.ServerPlayer j) { }
        };
    }

    // ------------------------------------------------------------------

    @Test
    @DisplayName("sem escolha, KO concentra no PUNHO -- nunca na cabeca")
    void koNasceNoPunho() {
        AlocacaoDeAura a = ko().alocacaoDesejada(FocoDeAura.padrao());

        assertEquals(DESTRO, a.maisConcentrada(),
                "Ko e o golpe. Concentrar na cabeca poe 95% da aura longe do punho "
                        + "e zera o reforco que o jogador acabou de pagar.");
        assertNotEquals(RegiaoDoCorpo.CABECA, a.maisConcentrada());
    }

    @Test
    @DisplayName("sem escolha, GYO concentra na CABECA -- e continua certo")
    void gyoNasceNaCabeca() {
        AlocacaoDeAura a = gyo().alocacaoDesejada(FocoDeAura.padrao());
        assertEquals(RegiaoDoCorpo.CABECA, a.maisConcentrada(),
                "Gyo nos olhos e a aplicacao famosa; o conserto do Ko nao pode "
                        + "levar o padrao do Gyo junto");
    }

    @Test
    @DisplayName("as duas tecnicas dao respostas DIFERENTES para a mesma ausencia")
    void oPadraoNaoPodeSerCompartilhado() {
        FocoDeAura semEscolha = FocoDeAura.padrao();
        assertNotEquals(
                gyo().alocacaoDesejada(semEscolha).maisConcentrada(),
                ko().alocacaoDesejada(semEscolha).maisConcentrada(),
                "um padrao unico so pode estar certo para uma das duas -- foi assim "
                        + "que o Ko acabou na cabeca");
    }

    @Test
    @DisplayName("o punho de Ko vem do JOGADOR: canhoto concentra a esquerda")
    void koRespeitaAMaoDominante() {
        FocoDeAura canhoto = new FocoDeAura(Optional.empty(), CANHOTO);
        assertEquals(CANHOTO, ko().alocacaoDesejada(canhoto).maisConcentrada(),
                "assumir destro poe a aura no braco errado de algumas pessoas, e "
                        + "isso nao da erro nenhum");
    }

    @Test
    @DisplayName("a escolha do jogador sobrescreve o padrao das duas")
    void oJogadorManda() {
        FocoDeAura escolheuPerna = FocoDeAura.de(RegiaoDoCorpo.PERNA_ESQUERDA, DESTRO);

        assertEquals(RegiaoDoCorpo.PERNA_ESQUERDA,
                ko().alocacaoDesejada(escolheuPerna).maisConcentrada());
        assertEquals(RegiaoDoCorpo.PERNA_ESQUERDA,
                gyo().alocacaoDesejada(escolheuPerna).maisConcentrada());
    }

    // ------------------------------------------------------------------
    // A distincao entre as duas tecnicas, em numeros
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Gyo REDISTRIBUI e Ko CONCENTRA -- a diferenca esta na conta")
    void gyoRedistribuiKoApostaTudo() {
        FocoDeAura foco = FocoDeAura.padrao();
        AlocacaoDeAura porGyo = gyo().alocacaoDesejada(foco);
        AlocacaoDeAura porKo = ko().alocacaoDesejada(foco);

        float focoDeGyo = porGyo.em(porGyo.maisConcentrada());
        float focoDeKo = porKo.em(porKo.maisConcentrada());

        assertTrue(focoDeKo > focoDeGyo * 1.5F,
                "Ko tem de ser visivelmente mais extremo que Gyo, senao as duas sao "
                        + "a mesma tecnica com constantes diferentes: Gyo=" + focoDeGyo
                        + " Ko=" + focoDeKo);

        // O RESTO DO CORPO E O QUE SEPARA AS DUAS. Com Gyo ainda ha defesa em
        // volta; com Ko nao ha quase nada, e e isso que torna errar catastrofico.
        float sobraDeGyo = porGyo.em(RegiaoDoCorpo.TRONCO);
        float sobraDeKo = porKo.em(RegiaoDoCorpo.TRONCO);
        assertTrue(sobraDeGyo > sobraDeKo * 3.0F,
                "o tronco sob Gyo tem de ficar bem mais protegido que sob Ko: "
                        + "Gyo=" + sobraDeGyo + " Ko=" + sobraDeKo);
    }

    @Test
    @DisplayName("as duas alocacoes somam 1 -- concentrar TIRA de algum lugar")
    void aAuraNaoSeMultiplica() {
        for (AlocacaoDeAura a : new AlocacaoDeAura[] {
                gyo().alocacaoDesejada(FocoDeAura.padrao()),
                ko().alocacaoDesejada(FocoDeAura.padrao())}) {
            float soma = 0.0F;
            for (RegiaoDoCorpo r : RegiaoDoCorpo.values()) {
                soma += a.em(r);
            }
            assertEquals(1.0F, soma, 0.0001F, "a alocacao deixou de somar 1");
        }
    }

    @Test
    @DisplayName("a aposta LITERAL cabe: 1.0 zera todo o resto do corpo")
    void aFracaoUmEUmaApostaValida() {
        // O range da config ja permite 1.0. O portao existe para garantir que a
        // conta aguenta o extremo sem quebrar -- e que ele significa o que diz.
        Ko extremo = new Ko(() -> 20.0D, () -> 1.0D, () -> 20, relogioMudo(), () -> 0.25D);
        AlocacaoDeAura a = extremo.alocacaoDesejada(FocoDeAura.padrao());

        assertEquals(1.0F, a.em(DESTRO), 0.0001F);
        assertEquals(0.0F, a.em(RegiaoDoCorpo.TRONCO), 0.0001F,
                "com 1.0 o resto do corpo fica literalmente sem aura");
    }

    @Test
    void focoIncompletoReprova() {
        assertThrows(IllegalArgumentException.class,
                () -> new FocoDeAura(null, DESTRO));
        assertThrows(IllegalArgumentException.class,
                () -> new FocoDeAura(Optional.empty(), null));
        assertThrows(IllegalArgumentException.class,
                () -> FocoDeAura.padrao().regiaoOu(null));
    }

    @Test
    void escolheuDizSeOJogadorApontou() {
        assertTrue(!FocoDeAura.padrao().escolheu());
        assertTrue(FocoDeAura.de(RegiaoDoCorpo.TRONCO, DESTRO).escolheu());
    }
}
