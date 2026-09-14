package com.darkcontinent.nenfoundation.enemy.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Sustenta as tres coisas que o King White Stag Beetle promete e que nenhuma
 * delas da erro quando quebra: a carapaca NUNCA paga, o ventre so paga nas duas
 * posturas em que o bicho o apresenta, e o tombo nao se renova.
 *
 * <p>Os numeros aqui sao proprios do teste, e nao os de
 * {@code KingWhiteStagBeetleTuning}: um teste que le o tuning passaria a medir a
 * sessao de balanceamento em vez da regra, e no dia em que alguem mexesse no
 * limiar ele continuaria verde sem ter verificado nada.</p>
 */
class RegrasDeViragemTest {

    /** Empinado: acima de 0.20 de altura e dentro de 60 graus (cos 0.5) de frente. */
    private static final WeakPointResolver EMPINADO =
            new WeakPointResolver("ventre", "carapaca", 0.20D, 0.5D);
    /** De costas: acima de 0.60 de altura, e SEM angulo nenhum. */
    private static final WeakPointResolver DE_COSTAS =
            new WeakPointResolver("ventre", "carapaca", 0.60D, -1.0D);

    /** 40 ticks de janela, dos quais os 10 finais sao o endireitar. */
    private static final RegrasDeViragem REGRAS =
            new RegrasDeViragem(EMPINADO, DE_COSTAS, 40, 10);

    // ------------------------------------------------------------- decidir

    @Test
    @DisplayName("o caso normal: trancar o stagger no aviso derruba o bicho")
    void trancoNoAvisoDerruba() {
        assertEquals(DecisaoDeViragem.VIRA_PELO_TRANCO,
                REGRAS.decidir(AttackPhase.WINDUP, true, false, false),
                "empinado ele se apoia so nas traseiras: e a pose que cai");
    }

    @Test
    @DisplayName("a investida que fecha sem encostar em ninguem derruba sozinha")
    void investidaErradaDerruba() {
        assertEquals(DecisaoDeViragem.VIRA_PELA_INVESTIDA,
                REGRAS.decidir(AttackPhase.ACTIVE, false, true, false),
                "e o premio de quem desviou, e a unica viragem que nao exige bater nele");
        assertEquals(DecisaoDeViragem.VIRA_PELA_INVESTIDA,
                REGRAS.decidir(AttackPhase.RECOVERY, true, true, false),
                "errar vence o tranco: quem desviou ja tinha derrubado o bicho");
    }

    @Test
    @DisplayName("tranco fora da pose empinada e stagger comum, e nao viragem")
    void trancoForaDoAvisoNaoDerruba() {
        for (AttackPhase fase : new AttackPhase[] { AttackPhase.IDLE, AttackPhase.ACTIVE,
                AttackPhase.RECOVERY, AttackPhase.COMPLETE }) {
            assertEquals(DecisaoDeViragem.SO_CAMBALEIA,
                    REGRAS.decidir(fase, true, false, false),
                    "em " + fase + " ele esta com as seis pernas no chao");
        }
        // Se esta asercao cair, QUALQUER interrupcao vira ponto fraco de graca: o
        // jogador deixa de precisar ler o telegrafo e o encontro se apaga.
    }

    @Test
    @DisplayName("sem tranco e sem investida errada, nada acontece com a postura")
    void semEventoNadaAcontece() {
        assertEquals(DecisaoDeViragem.SEGUE_DE_PE,
                REGRAS.decidir(AttackPhase.WINDUP, false, false, false),
                "bater no ventre exposto sem estourar o limiar NAO derruba");
        assertEquals(DecisaoDeViragem.SEGUE_DE_PE,
                REGRAS.decidir(AttackPhase.IDLE, false, false, false));
    }

    @Test
    @DisplayName("nao existe segundo tombo, nem nas condicoes perfeitas")
    void oSegundoTomboNuncaAcontece() {
        assertEquals(DecisaoDeViragem.JA_ESTA_DE_COSTAS,
                REGRAS.decidir(AttackPhase.WINDUP, true, true, true),
                "renovar a janela a cada golpe daria um chefe que nunca mais se levanta");
        assertEquals(DecisaoDeViragem.JA_ESTA_DE_COSTAS,
                REGRAS.decidir(AttackPhase.IDLE, false, false, true),
                "o marcador vence todas as outras perguntas, e vence primeiro");
    }

    @Test
    @DisplayName("fase ausente e erro de chamada, e nao uma decisao silenciosa")
    void faseNulaRecusaDecidir() {
        assertThrows(IllegalArgumentException.class,
                () -> REGRAS.decidir(null, true, false, false));
    }

    // -------------------------------------------------------------- regiao

    @Test
    @DisplayName("de pe e sem empinar, NADA paga -- nem o acerto alto e de frente")
    void carapacaNuncaPaga() {
        assertEquals("carapaca", REGRAS.regiao(AttackPhase.IDLE, false, 0.99D, 1.0D),
                "o alto da casca, de frente, continua sendo casca: e a ficha inteira do bicho");
        assertEquals("carapaca", REGRAS.regiao(AttackPhase.ACTIVE, false, 0.95D, 1.0D));
        assertEquals("carapaca", REGRAS.regiao(AttackPhase.RECOVERY, false, 0.95D, 1.0D),
                "na recuperacao ele ja esta com as seis no chao, e a casca volta a cobrir tudo");
        // O resolver do empinado tem altura minima 0.20: se ele fosse consultado
        // fora do aviso, TODO golpe frontal viraria critico e a armadura 9 deixaria
        // de significar alguma coisa -- sem erro nenhum, e com o bicho morrendo em
        // cinco espadadas.
    }

    @Test
    @DisplayName("empinado, a frente mole paga -- e so para quem esta na frente")
    void ventreEmpinadoExigeFrente() {
        assertEquals("ventre", REGRAS.regiao(AttackPhase.WINDUP, false, 0.80D, 0.9D));
        assertEquals("ventre", REGRAS.regiao(AttackPhase.WINDUP, false, 0.20D, 0.5D),
                "as duas bordas sao inclusivas: exatamente no limite ainda paga");
        assertEquals("carapaca", REGRAS.regiao(AttackPhase.WINDUP, false, 0.80D, 0.49D),
                "fora do arco frontal e casca, mesmo com ele empinado");
        assertEquals("carapaca", REGRAS.regiao(AttackPhase.WINDUP, false, 0.80D, -1.0D),
                "pelas costas, empinado, o que esta virado para o jogador e a casca");
        assertEquals("carapaca", REGRAS.regiao(AttackPhase.WINDUP, false, 0.10D, 1.0D),
                "rasante no pe plantado nao e barriga erguida");
    }

    @Test
    @DisplayName("de costas o ventre paga de QUALQUER angulo")
    void ventreDeCostasNaoTemAngulo() {
        for (double cosseno : new double[] { 1.0D, 0.0D, -0.5D, -1.0D }) {
            assertEquals("ventre", REGRAS.regiao(AttackPhase.IDLE, true, 0.90D, cosseno),
                    "um besouro de pernas para o ar nao tem frente (cos " + cosseno + ")");
        }
        assertEquals("ventre", REGRAS.regiao(AttackPhase.IDLE, true, 0.60D, -1.0D),
                "exatamente no limiar ainda paga");
    }

    @Test
    @DisplayName("de costas, o que encosta no chao continua sendo carapaca")
    void carapacaNoChaoNaoPaga() {
        assertEquals("carapaca", REGRAS.regiao(AttackPhase.IDLE, true, 0.59D, 1.0D),
                "abaixo do limiar e a casca, que agora e o lado de baixo");
        assertEquals("carapaca", REGRAS.regiao(AttackPhase.IDLE, true, 0.0D, 1.0D));
        // Sem esta metade sobraria zero regiao comum com o bicho virado, todo golpe
        // viraria critico e o ponto fraco deixaria de ser um ponto.
    }

    @Test
    @DisplayName("de costas vence a fase: virado nao ha aviso para ler")
    void deCostasVenceAFase() {
        assertEquals("carapaca", REGRAS.regiao(AttackPhase.WINDUP, true, 0.30D, 1.0D),
                "0.30 pagaria pelo resolver do empinado e nao paga pelo do virado");
    }

    // ----------------------------------------------------------- podeAtacar

    @Test
    @DisplayName("de cabeca para baixo ele nao ataca")
    void deCostasNaoAtaca() {
        assertTrue(REGRAS.podeAtacar(false));
        assertFalse(REGRAS.podeAtacar(true),
                "a janela so vale alguma coisa porque ela e segura: um besouro que"
                        + " golpeasse de costas trocaria o premio por uma troca de dano");
    }

    // -------------------------------------------------------- construcao

    @Test
    @DisplayName("regras impossiveis sao recusadas na construcao")
    void regrasImpossiveisSaoRejeitadas() {
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeViragem(EMPINADO,
                        new WeakPointResolver("ventre", "carapaca", 0.50D, -1.0D), 40, 10),
                "limiar em 0.50 com o bicho virado paga critico na casca encostada no chao");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeViragem(EMPINADO,
                        new WeakPointResolver("ventre", "carapaca", 0.60D, -0.5D), 40, 10),
                "cosseno acima de -1 de costas obriga o jogador a procurar uma frente que"
                        + " nao existe, e a janela queima enquanto ele circula");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeViragem(
                        new WeakPointResolver("ventre", "carapaca", 0.20D, 0.0D),
                        DE_COSTAS, 40, 10),
                "sem exigir frente, o ventre empinado passa a pagar pelo flanco");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeViragem(
                        new WeakPointResolver("barriga", "casca", 0.20D, 0.5D),
                        DE_COSTAS, 40, 10),
                "duas posturas com nomes de regiao diferentes: o catalogo so conhece um id"
                        + " e a outra postura deixa de pagar em silencio");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeViragem(EMPINADO, DE_COSTAS, 40, 40),
                "endireitar que ocupa a janela inteira e um aviso que comeca antes de haver"
                        + " o que avisar");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeViragem(EMPINADO, DE_COSTAS, 10, 40));
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeViragem(EMPINADO, DE_COSTAS, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeViragem(null, DE_COSTAS, 40, 10));
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeViragem(EMPINADO, null, 40, 10));
    }
}
