package com.darkcontinent.nenfoundation.client.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.api.SinalDeAura;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** O visual derivado do sinal de terceiros, e o LOD que so agora tem uso real. */
class EstadoVisualDeTerceiroTest {

    @Test
    @DisplayName("sinal NENHUM nao desenha nada, em nenhuma distancia")
    void nenhumNaoDesenha() {
        for (AuraRenderLod lod : AuraRenderLod.values()) {
            assertFalse(EstadoVisualDeTerceiro.de(SinalDeAura.NENHUM, lod).enabled(),
                    "NENHUM virou desenho no LOD " + lod + ". Este e o sinal que"
                            + " quem esta em Zetsu emite -- desenhar aqui anularia"
                            + " toda a proteccao do lado do servidor.");
        }
        assertFalse(EstadoVisualDeTerceiro.de(null, AuraRenderLod.FULL).enabled());
    }

    @Test
    @DisplayName("HIDDEN nao desenha, mesmo com sinal forte")
    void escondidoNaoDesenha() {
        assertFalse(EstadoVisualDeTerceiro.de(SinalDeAura.REN, AuraRenderLod.HIDDEN).enabled());
    }

    @Test
    @DisplayName("mais longe, mais fraco -- e nunca mais forte")
    void intensidadeCaiComADistancia() {
        // O LOD ATE AQUI NUNCA MORDEU EM JOGO: so existia o proprio jogador, e a
        // distancia dele para si mesmo e zero. Com aura de terceiros ele passa a
        // decidir de verdade, e por isso ganha teste de comportamento e nao so
        // de tabela.
        float perto = EstadoVisualDeTerceiro.de(SinalDeAura.REN, AuraRenderLod.FULL).intensity();
        float medio = EstadoVisualDeTerceiro.de(SinalDeAura.REN, AuraRenderLod.NEAR).intensity();
        float longe = EstadoVisualDeTerceiro.de(SinalDeAura.REN,
                AuraRenderLod.MEDIUM).intensity();

        assertTrue(perto > medio, "perto (" + perto + ") nao e mais forte que medio (" + medio + ")");
        assertTrue(medio > longe, "medio (" + medio + ") nao e mais forte que longe (" + longe + ")");
        assertTrue(longe > 0.0F,
                "no LOD mais baixo visivel a aura sumiu de vez. Quem esta longe"
                        + " deve continuar lendo 'aquela pessoa esta em Ren' --"
                        + " se some, o corte devia ter sido HIDDEN.");
    }

    @Test
    @DisplayName("Ten e Ren continuam distinguiveis a distancia")
    void tenERenSeparadosMesmoLonge() {
        var ten = EstadoVisualDeTerceiro.de(SinalDeAura.TEN, AuraRenderLod.MEDIUM);
        var ren = EstadoVisualDeTerceiro.de(SinalDeAura.REN, AuraRenderLod.MEDIUM);

        assertNotEquals(ten.mode(), ren.mode());
        assertNotEquals(ten.primaryColor(), ren.primaryColor(),
                "Ten e Ren sairam da mesma cor para terceiros. A cor e a mesma do"
                        + " HUD e da roda de proposito; se ela colapsa aqui, o"
                        + " jogador nao sabe o que esta olhando.");
        assertTrue(ren.preset().particleIntensity() > ten.preset().particleIntensity(),
                "Ren nao emite mais que Ten a distancia.");
    }

    @Test
    @DisplayName("o estado de terceiro ja nasce pronto, sem transicao pendente")
    void nasceCompleto() {
        // Terceiros NAO tem interpolador: o visual e derivado do sinal e some
        // junto com ele. Um estado nascendo com transicao pela metade ficaria
        // parado no meio da animacao para sempre, porque ninguem o avanca.
        assertEquals(1.0F,
                EstadoVisualDeTerceiro.de(SinalDeAura.REN, AuraRenderLod.FULL).transitionProgress(),
                1.0e-6F,
                "o estado de terceiro nasceu com transicao pendente, e nao ha"
                        + " quem a avance.");
    }
}
