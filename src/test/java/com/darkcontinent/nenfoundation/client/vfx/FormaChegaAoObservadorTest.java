package com.darkcontinent.nenfoundation.client.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.api.SinalDeAura;
import com.darkcontinent.nenfoundation.nen.aura.AlocacaoDeAura;
import com.darkcontinent.nenfoundation.nen.aura.RegiaoDoCorpo;
import com.darkcontinent.nenfoundation.network.payload.PresencaDeAuraS2C;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O portao da FORMA que atravessa a rede.
 *
 * <p><b>NASCEU DE UM DEFEITO VISTO EM JOGO (2026-09-26).</b> Gyo, Ko e Shu
 * concentram a aura num ponto. Para o proprio jogador funcionava; para quem
 * OLHAVA, o corpo inteiro acendia -- um Gyo lido como Ken, um Ko lido como Ren
 * de corpo cheio. A causa era uma linha:
 *
 * <pre>
 *   return new AuraVisualState(modo, intensidade, 1.0F,
 *           AuraDistribution.uniforme(), cor, cor);   // chumbado
 * </pre>
 *
 * <p>A alocacao existia, viajava no {@code nen_runtime_delta} e chegava ao
 * dono. O que faltava era ela chegar a quem observa.
 */
class FormaChegaAoObservadorTest {

    /**
     * A margem de erro da codificacao, DERIVADA e nao escolhida.
     *
     * <p>A primeira versao deste teste usava um passo de quantizacao
     * ({@code 1/255}) e reprovou com {@code 0.95 -> 0.9416}. Estava errada, e a
     * conta mostra por que: cada byte carrega ate {@code 0.5/255} de erro, a
     * soma de seis carrega ate {@code 3/255}, e a renormalizacao divide um
     * numero com erro por outro com erro. Para uma fracao alta, o termo que
     * domina e {@code f * 3/255} -- cerca de {@code 0.012} em {@code f = 0.95}.
     *
     * <p>Dois centesimos cobrem isso com folga e continuam apertados o
     * bastante para pegar uma regiao trocada, que e o defeito que importa. Um
     * passo de quantizacao era uma margem para OUTRA codificacao.
     */
    private static final float TOLERANCIA = 0.02F;

    /** Uma concentracao extrema no braco direito, como a de Ko. */
    private static AlocacaoDeAura noPunho() {
        return AlocacaoDeAura.concentrando(RegiaoDoCorpo.BRACO_DIREITO, 0.95F);
    }

    // ------------------------------------------------------------------
    // 1. O DEFEITO RELATADO
    // ------------------------------------------------------------------

    @Test
    @DisplayName("o observador NAO ve o corpo inteiro aceso quando ha concentracao")
    void aConcentracaoChegaAQuemOlha() {
        AuraDistribution forma = AuraDistribution.daAlocacao(noPunho());
        AuraVisualState visto = EstadoVisualDeTerceiro.de(
                SinalDeAura.REN, AuraRenderLod.FULL, 1.0F, forma);

        assertEquals(1.0F, visto.distribution().intensidade(AuraBodyRegion.RIGHT_ARM), 0.001F,
                "o punho concentrado tem de chegar cheio");
        assertTrue(visto.distribution().intensidade(AuraBodyRegion.TORSO) < 0.2F,
                "o tronco tem de ficar apagado. Se ele acender, o observador le um "
                        + "Ko como Ren de corpo cheio -- o defeito relatado");
        assertTrue(visto.distribution().intensidade(AuraBodyRegion.HEAD) < 0.2F);
    }

    @Test
    @DisplayName("sem forma declarada, o corpo inteiro continua acendendo")
    void aSobrecargaAntigaSegueUniforme() {
        // A sobrecarga de tres argumentos e o caminho de quem nao tem forma --
        // e ela precisa continuar devolvendo o corpo inteiro, senao uma aura
        // sem informacao de regiao ficaria invisivel.
        AuraVisualState semForma = EstadoVisualDeTerceiro.de(
                SinalDeAura.TEN, AuraRenderLod.FULL, 1.0F);
        for (AuraBodyRegion r : AuraBodyRegion.values()) {
            assertEquals(1.0F, semForma.distribution().intensidade(r), 0.001F, r.toString());
        }
    }

    @Test
    @DisplayName("forma nula nao derruba o desenho")
    void formaNulaCaiEmUniforme() {
        AuraVisualState visto = EstadoVisualDeTerceiro.de(
                SinalDeAura.TEN, AuraRenderLod.FULL, 1.0F, null);
        assertEquals(1.0F, visto.distribution().intensidade(AuraBodyRegion.TORSO), 0.001F);
    }

    // ------------------------------------------------------------------
    // 2. A TRAVESSIA -- o que entra no fio e o que sai
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a forma sobrevive a quantizacao com a PROPORCAO intacta")
    void aQuantizacaoPreservaOQueImporta() {
        AlocacaoDeAura original = noPunho();
        AlocacaoDeAura voltou = ida_e_volta(original);

        assertEquals(original.maisConcentrada(), voltou.maisConcentrada(),
                "a regiao dominante nao pode mudar no caminho");
        for (RegiaoDoCorpo r : RegiaoDoCorpo.values()) {
            assertEquals(original.em(r), voltou.em(r), TOLERANCIA,
                    "regiao " + r + " saiu da margem da codificacao");
        }
    }

    @Test
    @DisplayName("as seis regioes NAO trocam de lugar no fio")
    void aOrdemDoEnumEOContratoDoFio() {
        // Cada regiao vira dominante uma vez. Se os bytes fossem lidos fora de
        // ordem, a aura apareceria no membro errado -- bytes validos com
        // significado trocado, que nao levanta excecao nenhuma.
        for (RegiaoDoCorpo r : RegiaoDoCorpo.values()) {
            AlocacaoDeAura voltou = ida_e_volta(AlocacaoDeAura.concentrando(r, 0.9F));
            assertEquals(r, voltou.maisConcentrada(),
                    "a concentracao em " + r + " reapareceu em " + voltou.maisConcentrada());
        }
    }

    @Test
    @DisplayName("uniforme atravessa como uniforme")
    void oCasoComumNaoSeDeforma() {
        AlocacaoDeAura voltou = ida_e_volta(AlocacaoDeAura.uniforme());
        float primeira = voltou.em(RegiaoDoCorpo.CABECA);
        for (RegiaoDoCorpo r : RegiaoDoCorpo.values()) {
            assertEquals(primeira, voltou.em(r), 0.0001F,
                    "a aura sem concentracao ficou torta no caminho");
        }
    }

    @Test
    @DisplayName("a aposta literal atravessa: 1.0 chega com o resto em zero")
    void oExtremoSobrevive() {
        AlocacaoDeAura voltou = ida_e_volta(
                AlocacaoDeAura.concentrando(RegiaoDoCorpo.BRACO_ESQUERDO, 1.0F));
        assertEquals(1.0F, voltou.em(RegiaoDoCorpo.BRACO_ESQUERDO), 0.001F);
        assertEquals(0.0F, voltou.em(RegiaoDoCorpo.TRONCO), 0.001F);

        AuraDistribution desenhada = AuraDistribution.daAlocacao(voltou);
        assertEquals(0.0F, desenhada.intensidade(AuraBodyRegion.TORSO), 0.001F,
                "com tudo num braco, o resto do corpo tem de sumir da tela");
    }

    // ------------------------------------------------------------------
    // 3. AS GUARDAS
    // ------------------------------------------------------------------

    @Test
    @DisplayName("presenca sem forma e valida, e vale uniforme")
    void oConstrutorCurtoContinuaValendo() {
        PresencaDeAuraS2C p = new PresencaDeAuraS2C(7, SinalDeAura.TEN);
        assertEquals(AlocacaoDeAura.uniforme(), p.alocacao());
    }

    @Test
    void presencaIncompletaReprova() {
        assertThrows(IllegalArgumentException.class,
                () -> new PresencaDeAuraS2C(1, null, AlocacaoDeAura.uniforme()));
        assertThrows(IllegalArgumentException.class,
                () -> new PresencaDeAuraS2C(1, SinalDeAura.TEN, null));
    }

    @Test
    @DisplayName("Gyo e Ko chegam VISIVELMENTE diferentes ao observador")
    void asDuasNaoSeConfundemNaTela() {
        AuraDistribution porGyo = AuraDistribution.daAlocacao(ida_e_volta(
                AlocacaoDeAura.concentrando(RegiaoDoCorpo.CABECA, 0.45F)));
        AuraDistribution porKo = AuraDistribution.daAlocacao(ida_e_volta(noPunho()));

        // Sob Gyo o resto do corpo ainda acende bastante; sob Ko ele quase
        // apaga. Se os dois chegassem iguais, a forma teria viajado para nada.
        assertTrue(porGyo.intensidade(AuraBodyRegion.TORSO) > 0.2F,
                "Gyo redistribui: o resto do corpo continua visivel");
        assertTrue(porKo.intensidade(AuraBodyRegion.TORSO) < 0.1F,
                "Ko aposta: o resto do corpo quase some");
        assertNotEquals(porGyo.intensidade(AuraBodyRegion.TORSO),
                porKo.intensidade(AuraBodyRegion.TORSO));
    }

    /** Serializa e desserializa pelo mesmo caminho do payload. */
    private static AlocacaoDeAura ida_e_volta(AlocacaoDeAura original) {
        var buf = new io.netty.buffer.UnpooledByteBufAllocator(false).buffer();
        try {
            PresencaDeAuraS2C.STREAM_CODEC.encode(buf,
                    new PresencaDeAuraS2C(42, SinalDeAura.REN, original));
            return PresencaDeAuraS2C.STREAM_CODEC.decode(buf).alocacao();
        } finally {
            buf.release();
        }
    }
}
