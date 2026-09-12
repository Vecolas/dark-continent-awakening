package com.darkcontinent.nenfoundation.nen.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.nen.aura.AlocacaoDeAura;
import com.darkcontinent.nenfoundation.nen.aura.RegiaoDoCorpo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** A conta do lado ofensivo, provada inteira sem subir o jogo. */
class AtaqueDeNenTest {

    private static final RegiaoDoCorpo BRACO = RegiaoDoCorpo.BRACO_DIREITO;

    @Test
    @DisplayName("em repouso, o reforco e exatamente o numero da config")
    void normalizacao() {
        // SEM NORMALIZACAO, estar em repouso valeria UM SEXTO do numero escrito
        // na config -- e o numero da config deixaria de corresponder a coisa
        // nenhuma. Quem abrisse uma sessao de balanceamento giraria um botao
        // cujo efeito nao bate com o rotulo, e isso nao da erro.
        float r = AtaqueDeNen.reforco(0.25D, AlocacaoDeAura.uniforme(), BRACO, 3.0D);
        assertEquals(0.25F, r, 1.0E-5F,
                "a alocacao uniforme devia dar o reforco base exato.");
    }

    @Test
    @DisplayName("concentrar NO braco multiplica; concentrar fora quase zera")
    void aConcentracaoDecideOGolpe() {
        AlocacaoDeAura noBraco = AlocacaoDeAura.concentrando(BRACO, 0.90F);
        AlocacaoDeAura naCabeca = AlocacaoDeAura.concentrando(RegiaoDoCorpo.CABECA, 0.90F);

        float comBraco = AtaqueDeNen.reforco(0.25D, noBraco, BRACO, 3.0D);
        float comCabeca = AtaqueDeNen.reforco(0.25D, naCabeca, BRACO, 3.0D);

        assertTrue(comBraco > 1.0F,
                "concentrar quase toda a aura no punho devia mais que dobrar o"
                        + " golpe; deu " + comBraco);
        assertTrue(comCabeca < 0.10F,
                "concentrar na CABECA devia deixar o punho quase vazio; deu "
                        + comCabeca + ". Esta e a troca inteira de Gyo e de Ko: se"
                        + " concentrar em outro lugar nao custar golpe, concentrar"
                        + " no braco vira bonus sem preco.");
    }

    @Test
    @DisplayName("o teto absoluto vale mesmo com a config pedindo mais")
    void tetoAbsoluto() {
        // A config e limitada a 3.0 pelo proprio spec, mas ela nao e a unica
        // entrada: um datapack ou um valor herdado de outro mundo chega por
        // fora. O teto do codigo nao depende de ninguem se comportar.
        float r = AtaqueDeNen.reforco(100.0D, AlocacaoDeAura.concentrando(BRACO, 0.99F),
                BRACO, 999.0D);
        assertEquals((float) AtaqueDeNen.TETO_ABSOLUTO, r, 1.0E-5F,
                "o teto absoluto nao segurou: " + r + ". Sem ele, um numero mal"
                        + " escolhido produz o golpe que mata tudo de uma vez --"
                        + " e isso nao da erro, da um jogo sem combate.");
    }

    @Test
    @DisplayName("o golpe nunca encolhe, e NaN nao atravessa")
    void bordas() {
        assertEquals(10.0F, AtaqueDeNen.danoDepoisDoReforco(10.0F, 0.0F), 0.0F,
                "reforco zero mudou o dano.");
        assertEquals(10.0F, AtaqueDeNen.danoDepoisDoReforco(10.0F, -5.0F), 0.0F,
                "reforco negativo virou reducao; este lado so soma.");
        // `Math.max(0, NaN)` devolve NaN. Este projeto ja caiu nisso do outro
        // lado da conta, e o sintoma era dano NaN atravessando inteiro.
        assertEquals(0.0F, AtaqueDeNen.danoDepoisDoReforco(Float.NaN, 1.0F), 0.0F,
                "dano NaN atravessou o reforco.");
        assertEquals(0.0F, AtaqueDeNen.reforco(Double.NaN, AlocacaoDeAura.uniforme(),
                BRACO, 3.0D), 0.0F, "reforco NaN virou numero.");
        assertEquals(0.0F, AtaqueDeNen.reforco(0.25D, null, BRACO, 3.0D), 0.0F,
                "alocacao nula devia valer zero, e nao estourar.");
    }
}
