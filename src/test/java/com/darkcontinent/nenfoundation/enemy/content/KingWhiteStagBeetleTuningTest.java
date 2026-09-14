package com.darkcontinent.nenfoundation.enemy.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHitbox;
import com.darkcontinent.nenfoundation.enemy.combat.AttackPhase;
import com.darkcontinent.nenfoundation.enemy.combat.DecisaoDeViragem;
import com.darkcontinent.nenfoundation.enemy.combat.RegrasDeViragem;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerResult;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Os numeros que o besouro vai levar para o jogo, medidos contra a ficha dele --
 * e nao contra si mesmos.
 *
 * <p>{@code RegrasDeViragemTest} prova a REGRA com numeros proprios. Este arquivo
 * prova os VALORES DE PRODUCAO: que a combinacao declarada em
 * {@link KingWhiteStagBeetleTuning} e construivel, que a carapaca continua nao
 * pagando contra a armadura e o stagger que o perfil da, que o ventre paga o
 * bastante para derrubar o bicho dentro do aviso, e que a investida consegue
 * alcancar quem ela decidiu atacar.</p>
 *
 * <p>Nenhuma dessas coisas levanta erro quando quebra: o mod carrega, o besouro
 * nasce, investe, cambaleia e morre. So o que ele ENSINA deixa de funcionar.</p>
 */
class KingWhiteStagBeetleTuningTest {

    /** Espada de diamante em 1.21.1 -- do jogo, e nao deste bicho. */
    private static final float ESPADA_DE_DIAMANTE = 7.0F;
    /**
     * Ticks entre duas espadadas com a barra de ataque cheia (1.6 ataques por
     * segundo, arredondado para cima). Do jogo, e nao deste bicho.
     */
    private static final int TICKS_ENTRE_ESPADADAS = 13;

    @Test
    @DisplayName("os numeros declarados formam regras de viragem validas")
    void osNumerosDeProducaoSaoConstruiveis() {
        RegrasDeViragem regras = KingWhiteStagBeetleTuning.regrasDeViragem();
        assertEquals(KingWhiteStagBeetleTuning.TICKS_DE_COSTAS, regras.ticksDeCostas());
        assertEquals(KingWhiteStagBeetleTuning.TICKS_PARA_LEVANTAR, regras.ticksParaLevantar());
        assertEquals(KingWhiteStagBeetleTuning.ALTURA_MINIMA_DO_VENTRE_DE_COSTAS,
                regras.ventreDeCostas().alturaMinima());
        assertEquals(-1.0D, regras.ventreDeCostas().cossenoMinimo(),
                "de costas nao ha frente: o -1 e cobrado pelo construtor e conferido aqui"
                        + " sobre os valores que de fato vao para o jogo");
        // O construtor ja cobra continencia, nomes de regiao iguais e janela maior
        // que o endireitar. Esta chamada e o que garante que a cobranca de fato
        // roda sobre os numeros de PRODUCAO, e nao so sobre os do teste da regra.
    }

    @Test
    @DisplayName("a carapaca nunca acumula stagger, por aritmetica e nao por sinalizador")
    void espadadaNaCarapacaNuncaDerruba() {
        StaggerState stagger =
                new StaggerState(GreedIslandProfiles.kingWhiteStagBeetleStagger());
        for (int golpe = 0; golpe < 40; golpe++) {
            StaggerResult resultado =
                    stagger.acumular(StaggerState.SEM_ATAQUE, ESPADA_DE_DIAMANTE);
            assertNotEquals(StaggerResult.DISPAROU, resultado,
                    "a espadada " + golpe + " na casca nao pode interromper nada");
            for (int t = 0; t < TICKS_ENTRE_ESPADADAS; t++) {
                stagger.tick();
            }
        }
        assertEquals(0.0F, stagger.acumulado(),
                "o decaimento come mais do que a espadada poe: quarenta golpes na casca"
                        + " deixam o acumulado em zero, e e isso que 'a carapaca nao paga'"
                        + " significa em numeros");
    }

    @Test
    @DisplayName("duas espadadas no ventre exposto derrubam o bicho, e cabem no aviso")
    void duasEspadadasNoVentreDerrubam() {
        StaggerState stagger =
                new StaggerState(GreedIslandProfiles.kingWhiteStagBeetleStagger());
        float noVentre = ESPADA_DE_DIAMANTE * KingWhiteStagBeetleTuning.MULTIPLICADOR_DO_VENTRE;

        assertEquals(StaggerResult.ACUMULOU, stagger.acumular(StaggerState.SEM_ATAQUE, noVentre),
                "a primeira nao derruba: o jogador tem de acertar DUAS dentro do aviso");
        for (int t = 0; t < TICKS_ENTRE_ESPADADAS; t++) {
            stagger.tick();
        }
        assertEquals(StaggerResult.DISPAROU, stagger.acumular(StaggerState.SEM_ATAQUE, noVentre),
                "a segunda estoura o limiar mesmo depois do decaimento de treze ticks");

        assertTrue(TICKS_ENTRE_ESPADADAS < KingWhiteStagBeetleTuning.WINDUP_DA_INVESTIDA,
                "as duas espadadas precisam caber nos " + KingWhiteStagBeetleTuning.WINDUP_DA_INVESTIDA
                        + " ticks de aviso; nao cabendo, a unica jogada que derruba o bicho"
                        + " a pancada seria impossivel, e nada acusaria isso");

        assertEquals(DecisaoDeViragem.VIRA_PELO_TRANCO,
                KingWhiteStagBeetleTuning.regrasDeViragem()
                        .decidir(AttackPhase.WINDUP, true, false, false));
    }

    @Test
    @DisplayName("a investida alcanca quem ela decidiu atacar")
    void oAlcanceDeDecisaoCabeNoArranco() {
        AttackHitbox caixa = KingWhiteStagBeetleTuning.caixaDaInvestida();
        double alcanceUtil = caixa.maxZ() + KingWhiteStagBeetleTuning.MEIA_LARGURA_DE_UM_ALVO
                + KingWhiteStagBeetleTuning.AVANCO_ESTIMADO_DA_INVESTIDA;
        assertTrue(KingWhiteStagBeetleTuning.ALCANCE_DA_INVESTIDA <= alcanceUtil,
                "ele decide investir a " + KingWhiteStagBeetleTuning.ALCANCE_DA_INVESTIDA
                        + " e o arranco so cobre " + alcanceUtil + ": a investida erraria TODA"
                        + " vez, o besouro se derrubaria sozinho para sempre, e isso nao levanta"
                        + " erro nenhum -- parece um chefe quebrado");
    }

    @Test
    @DisplayName("a investida e telegrafada: o aviso e mais longo que a janela que machuca")
    void aInvestidaETelegrafada() {
        AttackDefinition investida = KingWhiteStagBeetleTuning.investida();
        assertTrue(investida.windupTicks() > investida.activeTicks(),
                "windup mais curto que a janela deixa de ser aviso e vira um mob que bate sem"
                        + " telegrafo, com o mesmo dano e o mesmo log limpo");
        assertTrue(investida.recoveryTicks() > investida.activeTicks(),
                "sem recuperacao longa nao ha janela de punicao, e desviar deixa de valer");
        assertTrue(KingWhiteStagBeetleTuning.TICKS_DE_MIRA_NO_WINDUP < investida.windupTicks(),
                "mirar o aviso inteiro daria um besouro que nunca erra, e VIRA_PELA_INVESTIDA"
                        + " deixaria de acontecer sem que nada acusasse");
        assertEquals(GreedIslandProfiles.kingWhiteStagBeetle().attributes().attackDamage(),
                investida.damage(),
                "o dano e LIDO do perfil: repetido aqui, girar o atributo numa sessao de"
                        + " balanceamento mudaria a barra de vida do jogador e nao este arquivo");
    }

    @Test
    @DisplayName("a recarga de interrupcao e mais longa que a recarga normal")
    void interromperTemDeValer() {
        assertTrue(KingWhiteStagBeetleTuning.RECARGA_APOS_INTERRUPCAO
                        > GreedIslandProfiles.kingWhiteStagBeetleRecarga(),
                "sem isso o besouro derrubado volta a investir mais depressa do que se"
                        + " ninguem tivesse batido, e o jogador aprende a nao interromper");
    }

    @Test
    @DisplayName("de pe ele nao tem ponto fraco, e o catalogo so conhece o ventre")
    void aCarapacaNaoEstaNoCatalogo() {
        assertEquals(1.0F, KingWhiteStagBeetleTuning.pontosFracos()
                        .multiplier(KingWhiteStagBeetleTuning.REGIAO_DA_CARAPACA),
                "a casca no catalogo, mesmo com multiplicador 1, seria um convite a alguem"
                        + " gira-lo para 1.2 e apagar a ficha do bicho");
        assertEquals(KingWhiteStagBeetleTuning.MULTIPLICADOR_DO_VENTRE,
                KingWhiteStagBeetleTuning.pontosFracos()
                        .multiplier(KingWhiteStagBeetleTuning.REGIAO_DO_VENTRE));
    }
}
