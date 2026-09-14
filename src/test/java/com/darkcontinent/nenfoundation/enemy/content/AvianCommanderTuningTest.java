package com.darkcontinent.nenfoundation.enemy.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.ai.OrdemDoComandante;
import com.darkcontinent.nenfoundation.enemy.ai.PosturaDeComando;
import com.darkcontinent.nenfoundation.enemy.ai.RegrasDeComandoAereo;
import com.darkcontinent.nenfoundation.enemy.ai.RegrasDeOrdemDeEsquadrao;
import com.darkcontinent.nenfoundation.enemy.ai.SituacaoDeComandoAereo;
import com.darkcontinent.nenfoundation.enemy.ai.SituacaoDeOrdem;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRules;
import com.darkcontinent.nenfoundation.enemy.chimera.ChimeraRank;
import com.darkcontinent.nenfoundation.enemy.chimera.ChimeraTrait;
import com.darkcontinent.nenfoundation.enemy.chimera.nen.TacticalNenIntent;
import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHitbox;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Os numeros que o Avian Commander vai levar para o jogo, medidos contra a ficha
 * dele -- e contra o DESENHO.
 *
 * <p>{@code RegrasDeComandoAereoTest} e {@code RegrasDeOrdemDeEsquadraoTest}
 * provam as REGRAS com numeros proprios. Este arquivo prova os VALORES DE
 * PRODUCAO: que a combinacao declarada em {@link AvianCommanderTuning} e
 * construivel, que a caixa do mergulho cabe na garra que foi desenhada, que o
 * alcance de mergulho nao come o alcance de visao, e que a janela de subida
 * continua sendo uma janela. Nenhuma dessas coisas levanta erro quando quebra --
 * o mod carrega, a comandante nasce, e so falha o que ela ensina.</p>
 */
class AvianCommanderTuningTest {

    /** O literal de ChimeraProfiles.avianCommander(): new EnemyAttributes(130, 0.32F, 15, 6, 36, 0.4F). */
    private static final float DANO_DA_FICHA = 15.0F;
    private static final double FOLLOW_RANGE_DA_FICHA = 36.0D;

    @Test
    @DisplayName("os numeros declarados formam regras de comando aereo validas")
    void osNumerosDeProducaoSaoConstruiveis() {
        RegrasDeComandoAereo voo = AvianCommanderTuning.comandoAereo();
        assertEquals(AvianCommanderTuning.ALTITUDE_DE_COMANDO, voo.altitudeDeComando());
        assertEquals(AvianCommanderTuning.ALTURA_DE_ABANDONO_DO_MERGULHO,
                voo.alturaDeAbandonoDoMergulho());
        assertEquals(AvianCommanderTuning.TICKS_DE_SUBIDA_APOS_MERGULHO,
                voo.ticksDeSubidaAposMergulho());
        // O construtor ja cobra a escada inteira (abandono < comando <= mergulho, e
        // janela >= 1). Esta chamada e o que garante que a cobranca roda sobre os
        // numeros de PRODUCAO, e nao so sobre os do teste da regra.
        RegrasDeOrdemDeEsquadrao ordens = AvianCommanderTuning.ordens();
        assertEquals(SquadRules.esquadrao(), ordens.bando());
        assertEquals(AvianCommanderTuning.MEMBROS_PARA_REAGRUPAR, ordens.membrosParaReagrupar());
    }

    @Test
    @DisplayName("o bando nao e redeclarado: teto, espacamento e orcamento saem de SquadRules")
    void oBandoNaoEhRedeclarado() {
        assertEquals(SquadRules.esquadrao(), AvianCommanderTuning.bando(),
                "Redeclarar o teto ou o orcamento aqui criaria dois numeros para a mesma coisa, e"
                        + " girar um deles numa sessao de balanceamento deixaria o outro mandando.");
    }

    @Test
    @DisplayName("a caixa do mergulho cabe na garra desenhada mais o impulso")
    void oMergulhoNaoAlcancaAlemDoDesenho() {
        AttackHitbox caixa = AvianCommanderTuning.caixaDoMergulho();
        double desenhoMaisImpulso = AvianCommanderTuning.ALCANCE_DESENHADO_DA_GARRA
                + AvianCommanderTuning.IMPULSO_DO_MERGULHO;
        assertTrue(caixa.maxZ() <= desenhoMaisImpulso,
                "A caixa vai a " + caixa.maxZ() + " blocos e a garra desenhada ("
                        + AvianCommanderTuning.ALCANCE_DESENHADO_DA_GARRA + ") mais o impulso ("
                        + AvianCommanderTuning.IMPULSO_DO_MERGULHO + ") chegam a "
                        + desenhoMaisImpulso + ". Caixa maior que o desenho da um jogador que"
                        + " apanha de uma garra que, na tela, parou antes dele -- dano certo,"
                        + " cooldown certo, log limpo.");
        assertTrue(caixa.minZ() <= AvianCommanderTuning.ALCANCE_DESENHADO_DA_GARRA,
                "A caixa comeca a " + caixa.minZ() + " blocos, adiante da ponta da garra: o golpe"
                        + " sairia de um ponto na frente das proprias garras.");
        // Esta e a outra ponta de um portao que morde dos dois lados. A primeira esta
        // em art-source/enemies/avian_commander/avian_commander_geo.py, em
        // valida_garra_alcanca_o_mergulho: la o desenho e medido contra estes mesmos
        // numeros. Quem encolher a garra reprova la; quem esticar a caixa reprova aqui.
    }

    @Test
    @DisplayName("a distancia de decisao cabe dentro do que a caixa alcanca")
    void decidirAtacarSoDentroDoQueAsGarrasAlcancam() {
        AttackHitbox caixa = AvianCommanderTuning.caixaDoMergulho();
        double alcanceReal = caixa.maxZ() + AvianCommanderTuning.MEIA_LARGURA_DE_UM_ALVO;
        assertTrue(AvianCommanderTuning.ALCANCE_DAS_GARRAS < alcanceReal,
                "Ela decide atacar a " + AvianCommanderTuning.ALCANCE_DAS_GARRAS + " blocos e a"
                        + " caixa so alcanca " + alcanceReal + ": ela comecaria um windup de "
                        + AvianCommanderTuning.WINDUP_DO_MERGULHO + " ticks contra alguem que ja"
                        + " esta fora de alcance, e o mergulho inteiro sairia para o vazio.");
    }

    @Test
    @DisplayName("o mergulho e mais curto que a visao: ha espaco em que ela VE e nao ataca")
    void verNaoEhMesmoQueAtacar() {
        assertTrue(AvianCommanderTuning.ALCANCE_DE_MERGULHO < FOLLOW_RANGE_DA_FICHA,
                "O alcance de mergulho (" + AvianCommanderTuning.ALCANCE_DE_MERGULHO + ") nao pode"
                        + " alcancar o followRange da ficha (" + FOLLOW_RANGE_DA_FICHA + "):"
                        + " iguais, 'ela viu voce' vira 'ela esta vindo', e o alcance 36 -- que e a"
                        + " unica coisa que a distingue de um oficial forte -- deixa de significar"
                        + " visao para significar agressao.");
    }

    @Test
    @DisplayName("o mergulho leva a comandante ate onde um golpe do chao alcanca")
    void oMergulhoDesceAteOAlcanceDeQuemEstaNoChao() {
        // Um jogador alcanca cerca de tres blocos. O mergulho tem de terminar DENTRO
        // disso, senao ela desce, machuca e sobe sem nunca ter estado ao alcance de
        // nada -- e "ela e vulneravel no chao" vira uma frase sem mecanica.
        assertTrue(AvianCommanderTuning.ALTURA_DE_ABANDONO_DO_MERGULHO < 3.0D,
                "O mergulho abandona a " + AvianCommanderTuning.ALTURA_DE_ABANDONO_DO_MERGULHO
                        + " blocos do chao, fora do alcance de quem esta em baixo.");
        assertTrue(AvianCommanderTuning.ALTURA_DE_ABANDONO_DO_MERGULHO > 0.0D,
                "Abandono em zero e um POUSO, e uma comandante pousada e um oficial de chao com"
                        + " asas.");
    }

    @Test
    @DisplayName("o golpe custa silencio: a subida dura mais que a recuperacao do ataque")
    void oMergulhoCustaOComando() {
        assertTrue(AvianCommanderTuning.TICKS_DE_SUBIDA_APOS_MERGULHO
                        > AvianCommanderTuning.RECUPERACAO_DO_MERGULHO,
                "A subida (" + AvianCommanderTuning.TICKS_DE_SUBIDA_APOS_MERGULHO + " ticks) tem"
                        + " de passar da recuperacao do ataque ("
                        + AvianCommanderTuning.RECUPERACAO_DO_MERGULHO + "): iguais ou menor, ela"
                        + " volta a comandar junto com o direito de atacar, e o mergulho deixa de"
                        + " custar alguma coisa.");
    }

    @Test
    @DisplayName("o telegrafo continua sendo um telegrafo, e mais longo que a janela")
    void oAvisoEhMaiorQueOGolpe() {
        AttackDefinition mergulho = AvianCommanderTuning.mergulho();
        assertEquals(DANO_DA_FICHA, mergulho.damage(),
                "O dano tem de sair da ficha, e nao de um numero repetido aqui: repetido, girar o"
                        + " atributo mudaria a barra de vida do jogador sem mudar este arquivo.");
        assertTrue(mergulho.windupTicks() > mergulho.activeTicks() * 2,
                "Windup " + mergulho.windupTicks() + " contra janela " + mergulho.activeTicks()
                        + ": um golpe que vem de sete blocos acima sem aviso e um golpe que"
                        + " ninguem le a tempo, e sair de baixo -- a resposta que o mob ensina --"
                        + " deixa de existir.");
        assertTrue(mergulho.interruptibleWindup(), "O aviso tem de ser interrompivel.");
        assertFalse(mergulho.interruptibleActive(),
                "Depois que as garras abrem, elas abrem: cancelar a janela apagaria um ataque que"
                        + " o jogador ja viu sair.");
        assertFalse(mergulho.interruptibleRecovery(),
                "Se o cambaleio cortasse a recuperacao, punir o golpe ACELERARIA a proxima ordem"
                        + " em vez de atrasa-la.");
    }

    @Test
    @DisplayName("interromper custa mais que a recarga normal")
    void interromperVale() {
        assertTrue(AvianCommanderTuning.RECARGA_APOS_INTERRUPCAO
                        > ChimeraProfiles.avianCommanderRecarga(),
                "Sem recarga maior, reset() devolveria a fase para IDLE e ela recomecaria no tick"
                        + " seguinte -- o jogador aprenderia a ignorar o cambaleio do unico mob em"
                        + " que o cambaleio muda o bando inteiro.");
    }

    @Test
    @DisplayName("a ficha continua a mesma: WINGS garantido, rank SQUADRON_LEADER, e nunca LEADER")
    void aFichaNaoMudouPorBaixo() {
        var molde = ChimeraProfiles.avianCommanderMolde();
        assertEquals(ChimeraRank.SQUADRON_LEADER, molde.rank());
        assertTrue(molde.traitsGarantidos().contains(ChimeraTrait.WINGS),
                "Sem WINGS garantido ela pode nascer sem asas, e uma comandante sem asas nao tem"
                        + " como chegar a altitude de comando: ela viraria um oficial de chao e"
                        + " NENHUM portao acusaria isso.");
        assertFalse(molde.papelNoSquad()
                        == com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRole.LEADER,
                "Lideranca e PROMOCAO em Squad. Um molde que ja nascesse lider daria dois lideres"
                        + " ao primeiro bando com duas dessas formigas.");
    }

    // ------------------------------------------------------------------ Nen

    @Test
    @DisplayName("Nen decide postura, e so postura: Zetsu e Ken vetam o mergulho, o resto nao")
    void nenVetaOMergulhoSemTocarEmAura() {
        assertFalse(AvianCommanderTuning.mergulhoPermitidoPor(TacticalNenIntent.ENTRAR_EM_ZETSU),
                "Mergulhar recolhida entregaria de graca o unico bicho do encontro que sabe sumir.");
        assertFalse(AvianCommanderTuning.mergulhoPermitidoPor(TacticalNenIntent.MANTER_KEN),
                "Largar Ken para atacar gastaria a defesa no instante em que ela e necessaria.");
        assertTrue(AvianCommanderTuning.mergulhoPermitidoPor(TacticalNenIntent.NENHUMA),
                "Exigir Nen para atacar deixaria toda formiga dormente inofensiva, e a colonia"
                        + " inteira e dormente no comeco.");
        assertTrue(AvianCommanderTuning.mergulhoPermitidoPor(TacticalNenIntent.MANTER_TEN));
        assertTrue(AvianCommanderTuning.mergulhoPermitidoPor(TacticalNenIntent.ELEVAR_REN));
        assertTrue(AvianCommanderTuning.mergulhoPermitidoPor(TacticalNenIntent.USAR_GYO));
    }

    @Test
    @DisplayName("REPROVA: intencao de Nen nula, que estouraria no meio do tick do servidor")
    void intencaoNulaEhRecusada() {
        assertThrows(NullPointerException.class,
                () -> AvianCommanderTuning.mergulhoPermitidoPor(null));
    }

    // ------------------- o caso que amarra as duas regras de producao -----

    @Test
    @DisplayName("com os numeros DE PRODUCAO, uma comandante no chao nao comanda")
    void noChaoElaNaoComandaComOsNumerosDeProducao() {
        RegrasDeComandoAereo voo = AvianCommanderTuning.comandoAereo();
        PosturaDeComando noChao = voo.decidir(new SituacaoDeComandoAereo(
                0.0D, 2.0D, true, false, AvianCommanderTuning.TICKS_DE_SUBIDA_APOS_MERGULHO, false));
        assertEquals(PosturaDeComando.SUBIR, noChao);

        OrdemDoComandante ordem = AvianCommanderTuning.ordens().decidir(new SituacaoDeOrdem(
                true, true, true, noChao, true, true, 5));
        assertEquals(OrdemDoComandante.BAIXA_DEMAIS, ordem,
                "Este e o encontro inteiro numa linha: trazida para baixo, ela para de comandar."
                        + " Se este caso passar a devolver TROCAR_ALVO, o mob continua nascendo,"
                        + " voando e atacando -- e a unica tatica que ele ensina some sem que nada"
                        + " mais reprove.");
    }

    @Test
    @DisplayName("com os numeros DE PRODUCAO, la do alto a ordem sai")
    void noAltoAOrdemSaiComOsNumerosDeProducao() {
        RegrasDeComandoAereo voo = AvianCommanderTuning.comandoAereo();
        PosturaDeComando noAlto = voo.decidir(new SituacaoDeComandoAereo(
                AvianCommanderTuning.ALTITUDE_DE_COMANDO + 1.0D,
                AvianCommanderTuning.ALCANCE_DE_MERGULHO + 1.0D, true, false,
                AvianCommanderTuning.TICKS_DE_SUBIDA_APOS_MERGULHO, false));
        assertEquals(PosturaDeComando.COMANDAR, noAlto);
        assertEquals(OrdemDoComandante.TROCAR_ALVO, AvianCommanderTuning.ordens().decidir(
                new SituacaoDeOrdem(true, true, true, noAlto, true, false, 5)));
    }
}
