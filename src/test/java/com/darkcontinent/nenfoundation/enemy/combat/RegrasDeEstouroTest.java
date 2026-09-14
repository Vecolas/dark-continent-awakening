package com.darkcontinent.nenfoundation.enemy.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Sustenta as tres coisas que o Hyper Puffball promete e que nenhuma delas da
 * erro quando quebra: o estouro acontece UMA vez, ele so conta toque de perto, e
 * a cadeia entre fungos nao comeca.
 *
 * <p>Os numeros aqui sao proprios do teste, e nao os de
 * {@code HyperPuffballTuning}: um teste que le o tuning passaria a medir a
 * sessao de balanceamento em vez da regra, e no dia em que alguem dobrasse o raio
 * ele continuaria verde sem ter verificado nada.</p>
 */
class RegrasDeEstouroTest {
    /** limiar 0.35, toque 3.5, aviso 5.5, raio 2.5, dano 6 -- a forma, nao os valores de producao. */
    private static final RegrasDeEstouro REGRAS =
            new RegrasDeEstouro(0.35F, 3.5D, 5.5D, 2.5D, 6.0F);

    private static final float VIDA_MAXIMA = 18.0F;
    /** 0.35 de 18 = 6.3: acima disso a casca aguenta. */
    private static final float ABAIXO_DO_LIMIAR = 6.0F;
    private static final float ACIMA_DO_LIMIAR = 7.0F;

    @Test
    @DisplayName("o caso normal: machucado de perto, abaixo do limiar, ele estoura")
    void estouraComVidaBaixaEAtacanteColado() {
        assertEquals(DecisaoDeEstouro.ESTOURA,
                REGRAS.decidir(ABAIXO_DO_LIMIAR, VIDA_MAXIMA, 2.0D, false));
        assertEquals(DecisaoDeEstouro.ESTOURA,
                REGRAS.decidir(0.0F, VIDA_MAXIMA, 1.0D, false),
                "o golpe que mata tambem estoura -- e essa e a conta de quem mata no soco");
        assertEquals(DecisaoDeEstouro.ESTOURA,
                REGRAS.decidir(VIDA_MAXIMA * 0.35F, VIDA_MAXIMA, 3.5D, false),
                "o limiar e as duas pontas sao inclusivos: exatamente na borda ainda estoura");
    }

    @Test
    @DisplayName("matar de longe evita o estouro -- a licao inteira do bicho")
    void flechaDeLongeNaoEstoura() {
        assertEquals(DecisaoDeEstouro.GATILHO_LONGE_DEMAIS,
                REGRAS.decidir(0.0F, VIDA_MAXIMA, 3.51D, false),
                "um centimetro alem do toque ja e tiro");
        assertEquals(DecisaoDeEstouro.GATILHO_LONGE_DEMAIS,
                REGRAS.decidir(0.0F, VIDA_MAXIMA, 20.0D, false));
        // Se esta asercao cair, o bicho passa a explodir em quem o matou com arco
        // do outro lado da clareira, e nada no jogo explica por que.
    }

    @Test
    @DisplayName("sem corpo nao ha estouro: fogo, queda e veneno nao disparam nada")
    void danoSemCausadorNaoEstoura() {
        assertEquals(DecisaoDeEstouro.SEM_ATACANTE,
                REGRAS.decidir(0.0F, VIDA_MAXIMA, Double.NaN, false));
    }

    @Test
    @DisplayName("a casca aguenta enquanto a vida esta acima do limiar")
    void vidaAltaRecusaOEstouro() {
        assertEquals(DecisaoDeEstouro.VIDA_ACIMA_DO_LIMIAR,
                REGRAS.decidir(ACIMA_DO_LIMIAR, VIDA_MAXIMA, 1.0D, false));
        assertEquals(DecisaoDeEstouro.VIDA_ACIMA_DO_LIMIAR,
                REGRAS.decidir(VIDA_MAXIMA, VIDA_MAXIMA, 0.5D, false));
    }

    @Test
    @DisplayName("nao existe segundo estouro, nem nas condicoes perfeitas")
    void oSegundoEstouroNuncaAcontece() {
        assertEquals(DecisaoDeEstouro.JA_ESTOUROU,
                REGRAS.decidir(0.0F, VIDA_MAXIMA, 0.5D, true),
                "estouro duplo nao da erro: da o dobro do dano documentado, sem log");
        assertEquals(DecisaoDeEstouro.JA_ESTOUROU,
                REGRAS.decidir(ACIMA_DO_LIMIAR, VIDA_MAXIMA, Double.NaN, true),
                "o marcador vence todas as outras perguntas, e vence primeiro");
    }

    @Test
    @DisplayName("a cadeia entre fungos e proibida na origem")
    void outroPuffballNuncaEVitima() {
        assertTrue(REGRAS.atinge(false, true, 1.0D), "um jogador colado leva o estouro");
        assertFalse(REGRAS.atinge(true, true, 1.0D),
                "outro puffball no raio NAO e vitima -- e assim que a cadeia nao comeca");
        assertFalse(REGRAS.atinge(false, false, 1.0D), "cadaver nao leva dano de area");
    }

    @Test
    @DisplayName("o estouro e redondo, e nao a caixa inflada que o procura")
    void foraDoRaioNinguemEAtingido() {
        assertTrue(REGRAS.atinge(false, true, 2.5D), "exatamente no raio ainda pega");
        assertFalse(REGRAS.atinge(false, true, 2.51D),
                "quem recuou na diagonal esta fora, mesmo estando dentro do AABB inflado");
        assertFalse(REGRAS.atinge(false, true, Double.NaN));
        assertFalse(REGRAS.atinge(false, true, -1.0D));
    }

    @Test
    @DisplayName("o aviso alcanca mais longe que o gatilho, e some sem alvo")
    void avisoCobreMaisQueOToque() {
        assertTrue(REGRAS.deveAvisar(5.5D), "na borda do aviso ele ja esta estufando");
        assertTrue(REGRAS.deveAvisar(3.5D),
                "na distancia do gatilho o aviso ja tem de estar rodando ha algum tempo");
        assertFalse(REGRAS.deveAvisar(5.51D));
        assertFalse(REGRAS.deveAvisar(Double.NaN), "sem alvo nao ha para quem estufar");
    }

    @Test
    @DisplayName("regras impossiveis sao recusadas na construcao")
    void regrasImpossiveisSaoRejeitadas() {
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeEstouro(0.35F, 3.5D, 3.0D, 2.5D, 6.0F),
                "aviso mais curto que o gatilho e um telegrafo que chega depois do perigo");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeEstouro(0.0F, 3.5D, 5.5D, 2.5D, 6.0F),
                "limiar zero seria um fungo que nunca estoura");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeEstouro(1.1F, 3.5D, 5.5D, 2.5D, 6.0F),
                "limiar acima de 1 estouraria com a vida cheia, ao primeiro arranhao");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeEstouro(0.35F, 0.0D, 5.5D, 2.5D, 6.0F));
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeEstouro(0.35F, 3.5D, 5.5D, 0.0D, 6.0F),
                "raio zero e um estouro que nao alcanca nem quem esta colado");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeEstouro(0.35F, 3.5D, 5.5D, 2.5D, 0.0F),
                "dano zero e a unica ameaca do bicho apagada em silencio");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeEstouro(Float.NaN, 3.5D, 5.5D, 2.5D, 6.0F));
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeEstouro(0.35F, Double.NaN, 5.5D, 2.5D, 6.0F));
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeEstouro(0.35F, 3.5D, 5.5D, Double.NaN, 6.0F));
    }

    @Test
    @DisplayName("vida invalida e erro de chamada, e nao uma decisao silenciosa")
    void vidaInvalidaRecusaDecidir() {
        assertThrows(IllegalArgumentException.class,
                () -> REGRAS.decidir(1.0F, 0.0F, 1.0D, false),
                "vida maxima zero dividiria a decisao por um limiar que nao existe");
        assertThrows(IllegalArgumentException.class,
                () -> REGRAS.decidir(Float.NaN, VIDA_MAXIMA, 1.0D, false),
                "NaN compara falso com tudo: ele viraria 'estoura' por acidente");
    }
}
