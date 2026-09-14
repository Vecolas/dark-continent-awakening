package com.darkcontinent.nenfoundation.enemy.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.chimera.ChimeraPeonDefinitions;
import com.darkcontinent.nenfoundation.enemy.chimera.ChimeraTrait;
import com.darkcontinent.nenfoundation.enemy.content.BatScoutTuning;
import com.darkcontinent.nenfoundation.enemy.content.ChimeraProfiles;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Sustenta que {@code NIGHT_VISION} FAZ alguma coisa, e diz exatamente o que.
 *
 * <p>Um trait que nao muda numero nenhum e decoracao: ele aparece no save, aparece
 * no debug, e o bicho se comporta igual ao vizinho que nao o tem. Isso nao da erro
 * -- da uma tabela de genes que o jogador nunca observa, e ninguem descobre porque
 * nao ha nada para descobrir. Esta bateria e o que impede o trait do batedor de
 * virar isso.</p>
 *
 * <p>O caso central e varrido pelos DEZESSEIS niveis de luz de proposito. Uma
 * implementacao que aplicasse o fator "so um pouquinho" para todo mundo, ou que
 * errasse a comparacao do limiar por um, passaria em qualquer teste de um nivel
 * so.</p>
 */
class RegrasDeVisaoNoturnaTest {

    private static final RegrasDeVisaoNoturna REGRAS = BatScoutTuning.visaoNoturna();

    /** O alcance declarado pelo perfil do Bat Scout: 32, o maior de qualquer peon. */
    private static final double ALCANCE =
            ChimeraProfiles.batScout().attributes().followRange();

    @Test
    @DisplayName("com visao noturna o alcance nao cai em NENHUM nivel de luz")
    void oCasoNormalNaoPerdeAlcanceNoEscuro() {
        for (int luz = 0; luz <= RegrasDeVisaoNoturna.LUZ_MAXIMA; luz++) {
            assertEquals(ALCANCE, REGRAS.alcanceEfetivo(ALCANCE, luz, true), 1.0E-9D,
                    "no nivel de luz " + luz + " o batedor continua enxergando os " + ALCANCE
                            + " blocos inteiros: e essa a consequencia do trait, e ela vale no"
                            + " escuro total tanto quanto ao meio-dia");
            assertFalse(REGRAS.oEscuroCobra(luz, true),
                    "o escuro nao pode cobrar nada de quem tem o trait, nem no nivel " + luz);
        }
    }

    @Test
    @DisplayName("sem visao noturna o escuro cobra, e cobra exatamente o fator declarado")
    void aRecusaEhPerderAlcanceNaPenumbra() {
        for (int luz = 0; luz < BatScoutTuning.LUZ_DE_PENUMBRA; luz++) {
            assertEquals(ALCANCE * BatScoutTuning.FATOR_NO_ESCURO_SEM_VISAO_NOTURNA,
                    REGRAS.alcanceEfetivo(ALCANCE, luz, false), 1.0E-9D,
                    "abaixo da penumbra, quem nao tem o trait enxerga so a fracao declarada;"
                            + " nivel de luz " + luz);
            assertTrue(REGRAS.oEscuroCobra(luz, false),
                    "e quem explica a decisao tem de ler a mesma coisa que o jogo faz");
        }
        for (int luz = BatScoutTuning.LUZ_DE_PENUMBRA;
                luz <= RegrasDeVisaoNoturna.LUZ_MAXIMA; luz++) {
            assertEquals(ALCANCE, REGRAS.alcanceEfetivo(ALCANCE, luz, false), 1.0E-9D,
                    "a partir do limiar ninguem perde alcance -- o trait so vale onde esta"
                            + " escuro, senao ele deixaria de ser visao NOTURNA; nivel " + luz);
        }
    }

    @Test
    @DisplayName("o limiar e inclusivo, e e ele que separa penumbra de escuro")
    void oLimiarEhInclusivo() {
        int limiar = BatScoutTuning.LUZ_DE_PENUMBRA;
        assertEquals(ALCANCE, REGRAS.alcanceEfetivo(ALCANCE, limiar, false), 1.0E-9D,
                "exatamente no limiar ainda se enxerga tudo");
        assertTrue(REGRAS.alcanceEfetivo(ALCANCE, limiar - 1, false) < ALCANCE,
                "um nivel abaixo ja cobra: um erro de um aqui nao da erro nenhum -- move o"
                        + " limiar de escuridao um bloco de tocha inteiro sem que nada acuse");
    }

    @Test
    @DisplayName("o trait do batedor e GARANTIDO: a regra so importa porque ele sempre o tem")
    void oMoldeGaranteOTraitQueEstaRegraTraduz() {
        // A regua que mede a regua: toda esta bateria fala sobre um trait que, se
        // sumisse do molde, deixaria o Bat Scout perdendo alcance a noite -- e os
        // testes acima continuariam verdes, porque eles passam o booleano a mao.
        assertTrue(ChimeraPeonDefinitions.batScout().traitsGarantidos()
                        .contains(ChimeraTrait.NIGHT_VISION),
                "o molde do bat_scout tem de GARANTIR NIGHT_VISION: sem garantia, metade das"
                        + " formigas nasceria cega no escuro e a colonia ficaria sem olho"
                        + " metade do tempo, sem que nada indicasse o motivo");
    }

    @Test
    @DisplayName("numero que apagaria a consequencia do trait reprova na construcao")
    void oConstrutorReprovaOQueNaoDariaErroEmJogo() {
        // ESTE E O CASO QUE DEVE REPROVAR. Fator 1.0 nao quebra nada visivel: o
        // codigo roda, o alcance sai, o log fica limpo. O que some e a diferenca
        // entre ter e nao ter o trait -- ou seja, a regra inteira. Ele e aceito no
        // construtor de proposito (o contrato e (0, 1]), e por isso o caso abaixo
        // cobra a CONSEQUENCIA em vez do construtor.
        RegrasDeVisaoNoturna inofensiva = new RegrasDeVisaoNoturna(1.0D, 7);
        assertFalse(inofensiva.oEscuroCobra(0, false),
                "com fator 1.0 o escuro nao cobra de ninguem, e o trait vira decoracao: este"
                        + " caso existe para que a possibilidade fique ESCRITA, e nao para"
                        + " aprova-la");
        assertTrue(BatScoutTuning.FATOR_NO_ESCURO_SEM_VISAO_NOTURNA < 1.0D,
                "o fator de producao tem de ser menor que 1, senao a regra do batedor nao"
                        + " significa nada");

        assertThrows(IllegalArgumentException.class, () -> new RegrasDeVisaoNoturna(0.0D, 7),
                "fator zero cega o mob por completo no escuro, e ele para de reagir sem uma"
                        + " linha de log");
        assertThrows(IllegalArgumentException.class, () -> new RegrasDeVisaoNoturna(1.5D, 7),
                "acima de 1 o escuro passaria a AUMENTAR o alcance, e a consequencia do trait"
                        + " viraria o contrario do que ele diz");
        assertThrows(IllegalArgumentException.class, () -> new RegrasDeVisaoNoturna(Double.NaN, 7),
                "NaN aprovaria nada e reprovaria nada, e ninguem saberia qual dos dois");
        assertThrows(IllegalArgumentException.class, () -> new RegrasDeVisaoNoturna(0.5D, 16),
                "limiar acima de 15 faria todo mob sem o trait enxergar menos em pleno dia");
        assertThrows(IllegalArgumentException.class, () -> new RegrasDeVisaoNoturna(0.5D, -1),
                "limiar negativo nao existe em nivel de luz nenhum");
    }

    @Test
    @DisplayName("leitura impossivel reprova onde foi medida, e nao no meio do cone de visao")
    void medidaInvalidaReprova() {
        assertThrows(IllegalArgumentException.class,
                () -> REGRAS.alcanceEfetivo(0.0D, 15, true),
                "alcance base zero produziria um cone que nao enxerga nada, e o sintoma seria"
                        + " um mob parado ao lado do jogador");
        assertThrows(IllegalArgumentException.class,
                () -> REGRAS.alcanceEfetivo(Double.NaN, 15, true),
                "alcance NaN atravessaria ate o cone de visao e la ninguem saberia de onde veio");
        assertThrows(IllegalArgumentException.class,
                () -> REGRAS.alcanceEfetivo(ALCANCE, 16, true),
                "luz 16 nao existe: quem chamou mediu outra coisa, e clampar em silencio"
                        + " esconderia a medida errada");
        assertThrows(IllegalArgumentException.class,
                () -> REGRAS.alcanceEfetivo(ALCANCE, -1, false),
                "luz negativa e a mesma medida errada, escrita de outro jeito");
    }
}
