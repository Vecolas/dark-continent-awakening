package com.darkcontinent.nenfoundation.enemy.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.chimera.nen.TacticalNenIntent;
import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.GolpeEncadeado;
import com.darkcontinent.nenfoundation.enemy.combat.SequenciaDeGolpes;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerResult;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Os numeros que a centopeia vai levar para o jogo, medidos contra a ficha dela --
 * e nao contra si mesmos.
 *
 * <p>{@code SequenciaDeGolpesTest} prova a REGRA com numeros proprios. Este
 * arquivo prova os VALORES DE PRODUCAO: que a sequencia declarada e construivel,
 * que o combo inteiro nao passa do teto declarado, que a distancia de decisao cabe
 * no alcance do PRIMEIRO golpe, que a mira cabe no MENOR dos tres avisos, e quanto
 * custa cortar o combo com o stagger do perfil.</p>
 *
 * <p>Nenhuma dessas coisas levanta erro quando quebra: o mod carrega, a formiga
 * nasce, ataca tres vezes, cambaleia e morre. So o que ela ENSINA deixa de
 * funcionar.</p>
 */
class MultiarmCentipedeTuningTest {

    /** Espada de diamante em 1.21.1 -- do jogo, e nao deste bicho. */
    private static final float ESPADA_DE_DIAMANTE = 7.0F;
    /**
     * Ticks entre duas espadadas com a barra de ataque cheia (1.6 ataques por
     * segundo, arredondado para cima). Do jogo, e nao deste bicho.
     */
    private static final int TICKS_ENTRE_ESPADADAS = 13;

    private static float danoDaFicha() {
        return ChimeraProfiles.multiarmCentipede().attributes().attackDamage();
    }

    // --------------------------------------------------------- a sequencia

    @Test
    @DisplayName("os numeros declarados formam uma sequencia valida de tres golpes")
    void osNumerosDeProducaoSaoConstruiveis() {
        SequenciaDeGolpes sequencia = MultiarmCentipedeTuning.sequencia();
        assertEquals(3, sequencia.quantidade());
        assertEquals(MultiarmCentipedeTuning.TICKS_DE_EMENDA, sequencia.ticksDeEmenda());
        assertEquals("braco_traseiro", sequencia.golpe(0).definicao().id());
        assertEquals("braco_dianteiro", sequencia.ultimo().definicao().id());
        // O construtor ja cobra telegrafo crescente, recuperacao crescente, emenda
        // que cabe e alcance que nao encolhe. Esta chamada e o que garante que a
        // cobranca de fato roda sobre os numeros de PRODUCAO, e nao so sobre os do
        // teste da regra.
    }

    @Test
    @DisplayName("o ULTIMO golpe e o unico com janela de punicao de verdade")
    void soOUltimoGolpeTemJanelaDePunicao() {
        SequenciaDeGolpes sequencia = MultiarmCentipedeTuning.sequencia();
        int emenda = sequencia.ticksDeEmenda();
        for (int i = 0; i < sequencia.quantidade() - 1; i++) {
            AttackDefinition encadeado = sequencia.golpe(i).definicao();
            assertTrue(encadeado.recoveryTicks() > emenda,
                    "a recuperacao de '" + encadeado.id() + "' precisa sobreviver a emenda, senao o"
                            + " encadeado chega depois de COMPLETE e o combo nunca acontece");
            assertTrue(sequencia.ultimo().definicao().recoveryTicks()
                            >= encadeado.recoveryTicks() * 3,
                    "a janela de punicao do ultimo golpe tem de ser MUITO maior que a recuperacao"
                            + " cortada dos encadeados; se elas ficarem parecidas, esperar a ultima"
                            + " deixa de pagar e a licao do encontro desaparece sem sintoma");
        }
    }

    @Test
    @DisplayName("o combo inteiro nao passa do teto declarado em multiplos do golpe da ficha")
    void oComboInteiroCabeNoTeto() {
        float teto = danoDaFicha() * MultiarmCentipedeTuning.TETO_DA_SEQUENCIA_EM_GOLPES;
        float total = MultiarmCentipedeTuning.sequencia().danoTotal();
        assertTrue(total <= teto,
                "a sequencia inteira tira " + total + " e o teto e " + teto + " ("
                        + MultiarmCentipedeTuning.TETO_DA_SEQUENCIA_EM_GOLPES + "x o golpe da"
                        + " ficha): acima disso o encontro deixa de ter margem para erro e o"
                        + " jogador nao consegue dizer qual dos tres golpes o matou");
        assertTrue(total > danoDaFicha(),
                "se o combo inteiro doer menos que um golpe unico, encadear tres janelas so tornou"
                        + " o mob mais fraco e mais complicado -- e isso nao aparece em portao"
                        + " nenhum");
    }

    @Test
    @DisplayName("o golpe final vale o dano cheio e os encadeados valem a fracao declarada")
    void oGolpeFinalEOQueDoi() {
        SequenciaDeGolpes sequencia = MultiarmCentipedeTuning.sequencia();
        assertEquals(danoDaFicha(), sequencia.ultimo().definicao().damage(), 1.0E-4F);
        float esperado = danoDaFicha() * MultiarmCentipedeTuning.FRACAO_DO_GOLPE_ENCADEADO;
        assertEquals(esperado, sequencia.golpe(0).definicao().damage(), 1.0E-4F);
        assertTrue(sequencia.golpe(0).definicao().damage() < sequencia.ultimo().definicao().damage(),
                "o combo precisa doer PROGRESSIVAMENTE: com o primeiro golpe tao caro quanto o"
                        + " ultimo, nao ha motivo para o jogador tentar sair do meio dele");
    }

    // ------------------------------------------------------------- alcance

    @Test
    @DisplayName("a distancia de decisao cabe no alcance do PRIMEIRO golpe")
    void aDecisaoCabeNoPrimeiroGolpe() {
        double alcanceDoPrimeiro = MultiarmCentipedeTuning.sequencia().alcanceParaComecarEmBlocos();
        double limite = alcanceDoPrimeiro + MultiarmCentipedeTuning.MEIA_LARGURA_DE_UM_ALVO;
        assertTrue(MultiarmCentipedeTuning.ALCANCE_DA_SEQUENCIA <= limite,
                "ela decide comecar a " + MultiarmCentipedeTuning.ALCANCE_DA_SEQUENCIA
                        + " blocos e o primeiro golpe so alcanca ate " + limite + " (borda do"
                        + " alvo): os dois primeiros golpes bateriam no ar sempre, e como a"
                        + " navegacao trava durante o combo, o sintoma seria um oficial que 'as"
                        + " vezes nao faz nada'");
    }

    @Test
    @DisplayName("o alcance cresce do primeiro ao ultimo golpe")
    void oAlcanceCresceAoLongoDoCombo() {
        SequenciaDeGolpes sequencia = MultiarmCentipedeTuning.sequencia();
        double anterior = 0.0D;
        for (int i = 0; i < sequencia.quantidade(); i++) {
            GolpeEncadeado golpe = sequencia.golpe(i);
            assertTrue(golpe.alcanceEmBlocos() > anterior,
                    "o golpe '" + golpe.definicao().id() + "' tem de alcancar mais que o anterior:"
                            + " com o alcance caindo, recuar um passo depois do primeiro tira o"
                            + " jogador de todos os seguintes");
            anterior = golpe.alcanceEmBlocos();
        }
    }

    // ------------------------------------------------------- mira e recarga

    @Test
    @DisplayName("a mira cabe dentro do MENOR dos tres avisos")
    void aMiraCabeNoMenorAviso() {
        SequenciaDeGolpes sequencia = MultiarmCentipedeTuning.sequencia();
        int menorAviso = Integer.MAX_VALUE;
        for (int i = 0; i < sequencia.quantidade(); i++) {
            menorAviso = Math.min(menorAviso, sequencia.golpe(i).definicao().windupTicks());
        }
        assertTrue(MultiarmCentipedeTuning.TICKS_DE_MIRA_NO_WINDUP < menorAviso,
                "ela mira por " + MultiarmCentipedeTuning.TICKS_DE_MIRA_NO_WINDUP + " ticks e o"
                        + " aviso mais curto dura " + menorAviso + ": com a mira cobrindo o aviso"
                        + " inteiro, aquele golpe vira mira-laser -- ela gira junto com quem"
                        + " desvia, o desvio deixa de existir e o telegrafo vira decoracao");
        assertTrue(MultiarmCentipedeTuning.TICKS_DE_MIRA_NO_WINDUP > 0,
                "nao mirar nada da o defeito oposto: um oficial que erra sozinho");
    }

    @Test
    @DisplayName("interromper vale mais do que esperar o combo terminar")
    void aRecargaPunitivaEMaiorQueANormal() {
        assertTrue(MultiarmCentipedeTuning.RECARGA_APOS_INTERRUPCAO
                        > ChimeraProfiles.multiarmCentipedeRecarga(),
                "a recarga apos interrupcao (" + MultiarmCentipedeTuning.RECARGA_APOS_INTERRUPCAO
                        + ") tem de superar a recarga normal ("
                        + ChimeraProfiles.multiarmCentipedeRecarga() + "): igual ou menor, cortar"
                        + " um combo de tres golpes nao paga nada, e o jogador aprende a NAO"
                        + " interromper -- o oposto do que o stagger existe para ensinar");
    }

    // --------------------------------------------- quanto custa cortar o combo

    @Test
    @DisplayName("cortar o combo e caro e POSSIVEL, e esta e a regua que diz quanto")
    void quantoCustaCortarOCombo() {
        // O stagger deste mob mora em ChimeraProfiles e NAO e um numero deste
        // arquivo. Esta regua existe para o custo da interrupcao ser MEDIDO em vez
        // de suposto: alto demais e ninguem corta o combo (a recompensa mais cara
        // do encontro nunca acontece); baixo demais e todo golpe corta (a sequencia
        // deixa de existir). Nenhum dos dois da erro.
        //
        // ESTE CASO MUDOU DE LADO, e vale saber por que. A versao anterior exigia
        // que a espadada comum fosse ABSORVIDA e que cortar custasse MAIS por golpe
        // do que o proprio golpe da formiga (13). Nenhuma arma vanilla chega la --
        // so um golpe de Nen -- entao a regra que parecia proteger a sequencia na
        // verdade garantia que a interrupcao nunca acontecesse. Um sistema que nao
        // dispara nunca e indistinguivel de um sistema quebrado, e a regua que o
        // abencoava era a prova de que ele estava certo.
        //
        // A intencao ("tres espadadas nao podem cortar o combo") continua cobrada
        // aqui. O que saiu foi o MECANISMO que a tornava impossivel de satisfazer
        // sem matar o sistema.
        StaggerState stagger = new StaggerState(ChimeraProfiles.multiarmCentipedeStagger());
        for (int espadada = 1; espadada <= 3; espadada++) {
            assertNotEquals(StaggerResult.DISPAROU,
                    stagger.acumular(StaggerState.SEM_ATAQUE, ESPADA_DE_DIAMANTE),
                    "a espadada " + espadada + " cortou o combo: com tres golpes bastando, a"
                            + " sequencia de tres bracos deixa de existir como decisao e vira"
                            + " decoracao");
            for (int tick = 0; tick < TICKS_ENTRE_ESPADADAS; tick++) stagger.tick();
        }

        int golpes = espadadasAteCortarOCombo();
        assertTrue(golpes > 3,
                "cortar o combo levou " + golpes + " espadadas: abaixo de quatro a interrupcao"
                        + " vira a resposta trivial e o telegrafo longo do ultimo golpe deixa de"
                        + " ensinar qualquer coisa");
        assertTrue(golpes <= 10,
                "cortar o combo levou " + golpes + " espadadas SEGUIDAS, sem errar nenhuma."
                        + " Acima de dez isso e o mesmo que nao existir, e e pior do que nao"
                        + " existir: o numero esta la e girar o botao nao muda nada.");
    }

    /** Quantas espadadas seguidas, no ritmo do jogador, cortam o combo. */
    private static int espadadasAteCortarOCombo() {
        StaggerState stagger = new StaggerState(ChimeraProfiles.multiarmCentipedeStagger());
        for (int espadada = 1; espadada <= 100; espadada++) {
            if (stagger.acumular(StaggerState.SEM_ATAQUE, ESPADA_DE_DIAMANTE)
                    == StaggerResult.DISPAROU) {
                return espadada;
            }
            for (int tick = 0; tick < TICKS_ENTRE_ESPADADAS; tick++) stagger.tick();
        }
        throw new AssertionError("cem espadadas seguidas nao cortaram o combo: a interrupcao e"
                + " impossivel por aritmetica, e nada no jogo acusaria isso");
    }

    // ------------------------------------------------------------- Nen

    @Test
    @DisplayName("a intencao de Nen vira POSTURA, e so Zetsu faz ela desistir do combo")
    void aIntencaoDeNenViraPosturaENaoPoder() {
        assertTrue(MultiarmCentipedeTuning.recuaEmVezDeAtacar(TacticalNenIntent.ENTRAR_EM_ZETSU),
                "quem acabou de decidir sumir nao comeca um combo que a prende no lugar");
        for (TacticalNenIntent intencao : TacticalNenIntent.values()) {
            if (intencao == TacticalNenIntent.ENTRAR_EM_ZETSU) continue;
            assertTrue(!MultiarmCentipedeTuning.recuaEmVezDeAtacar(intencao),
                    "a intencao " + intencao + " nao pode mudar o combo: traduzir REN ou KEN em"
                            + " 'ataca mais' seria um efeito de Nen decidido fora do nucleo, e o"
                            + " CLAUDE.md diz que o Nen Foundation e a unica autoridade sobre"
                            + " Nen -- duas autoridades divergem sem dar erro");
        }
    }

    @Test
    @DisplayName("a fracao de aura da formiga e ZERO enquanto o nucleo nao publicar pool")
    void aAuraDaFormigaEUmPontoCegoDeclaradoENaoUmNumeroInventado() {
        assertEquals(0.0D, MultiarmCentipedeTuning.FRACAO_DE_AURA_NAO_PUBLICADA,
                "qualquer valor diferente de zero aqui seria a formiga afirmando ter aura que"
                        + " ninguem calculou -- a segunda autoridade sobre Nen que o CLAUDE.md"
                        + " proibe. Este teste existe para que subir este numero 'so para testar'"
                        + " reprove o build em vez de virar balanceamento fantasma");
        assertNotEquals(true, TacticalNenIntent.NENHUMA.usaNen(),
                "NENHUMA continua sendo a intencao que nao gasta Nen nenhum");
    }
}
