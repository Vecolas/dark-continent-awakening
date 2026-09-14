package com.darkcontinent.nenfoundation.enemy.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.ai.RegrasDeArranque;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRules;
import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHitbox;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Os numeros que o Cheetah Leader vai levar para o jogo, medidos contra a ficha
 * dele -- e contra o DESENHO.
 *
 * <p>{@code RegrasDeArranqueTest} prova a REGRA com numeros proprios. Este
 * arquivo prova os VALORES DE PRODUCAO: que a combinacao declarada em
 * {@link CheetahLeaderTuning} e construivel, que a caixa do bote cabe no focinho
 * que foi desenhado, e que o arranque continua sendo um arranque com preco.
 * Nenhuma dessas coisas levanta erro quando quebra -- o mod carrega, a formiga
 * nasce, e so falha o que ela ensina.</p>
 */
class CheetahLeaderTuningTest {

    /** O literal de ChimeraProfiles.cheetahLeader(): new EnemyAttributes(95, 0.46F, 14, 3, 34, 0.2F). */
    private static final float DANO_DA_FICHA = 14.0F;
    private static final double ALCANCE_DE_PERCEPCAO_DA_FICHA = 34.0D;

    @Test
    @DisplayName("os numeros declarados formam regras de arranque validas")
    void osNumerosDeProducaoSaoConstruiveis() {
        RegrasDeArranque arranque = CheetahLeaderTuning.arranque();
        assertEquals(CheetahLeaderTuning.TICKS_DE_ARRANQUE, arranque.ticksDeArranque());
        assertEquals(CheetahLeaderTuning.TICKS_DE_FADIGA, arranque.ticksDeFadiga());
        assertEquals(CheetahLeaderTuning.TICKS_DE_RECARGA, arranque.ticksDeRecarga());
        assertEquals(CheetahLeaderTuning.DISTANCIA_DE_ABERTURA, arranque.distanciaDeAbertura());
        // O construtor ja cobra a fadiga minima, a recarga e a media do ciclo.
        // Esta chamada e o que garante que a cobranca de fato roda sobre os numeros
        // de PRODUCAO, e nao so sobre os do teste da regra.
    }

    @Test
    @DisplayName("o arranque REDISTRIBUI velocidade: no ciclo ela nao ganha terreno")
    void oArranqueDeProducaoNaoCriaVelocidade() {
        RegrasDeArranque arranque = CheetahLeaderTuning.arranque();
        assertTrue(arranque.mediaDoCiclo() <= RegrasDeArranque.TETO_DA_MEDIA_DO_CICLO,
                "A media do ciclo e " + arranque.mediaDoCiclo() + ", acima do teto de "
                        + RegrasDeArranque.TETO_DA_MEDIA_DO_CICLO + ". Acima de 1.0 o arranque"
                        + " passa a criar velocidade em vez de redistribui-la: ninguem consegue"
                        + " fugir, a fadiga vira encenacao, e nada no log explica o porque.");
        assertTrue(arranque.ticksDeFadiga() >= RegrasDeArranque.FADIGA_MINIMA,
                "A fadiga e a JANELA DE RESPOSTA do jogador. Abaixo de "
                        + RegrasDeArranque.FADIGA_MINIMA + " ticks ela existe so no servidor.");
    }

    @Test
    @DisplayName("o arranque nao e autorizado alem do que ela consegue perceber")
    void arrancarSoContraQuemElaPodeVer() {
        assertTrue(CheetahLeaderTuning.DISTANCIA_MAXIMA_DO_ARRANQUE
                        < ALCANCE_DE_PERCEPCAO_DA_FICHA,
                "Autorizar o arranque a " + CheetahLeaderTuning.DISTANCIA_MAXIMA_DO_ARRANQUE
                        + " blocos com percepcao de " + ALCANCE_DE_PERCEPCAO_DA_FICHA
                        + " daria um limite que nunca e alcancado: o numero viraria letra morta e"
                        + " a proxima sessao de balanceamento giraria um botao que nao liga em"
                        + " nada.");
        assertEquals(ALCANCE_DE_PERCEPCAO_DA_FICHA,
                ChimeraProfiles.cheetahLeader().attributes().followRange(),
                "O literal deste teste envelheceu em relacao a ficha publicada. As duas pontas"
                        + " precisam bater, senao a comparacao acima passa a medir um alcance que o"
                        + " bicho nao tem.");
    }

    @Test
    @DisplayName("a caixa do bote cabe no focinho desenhado mais o avanco")
    void oBoteNaoAlcancaAlemDoDesenho() {
        AttackHitbox caixa = CheetahLeaderTuning.caixaDoBote();
        double desenhoMaisAvanco = CheetahLeaderTuning.ALCANCE_DESENHADO_DA_FRENTE
                + CheetahLeaderTuning.AVANCO_DO_BOTE;
        assertTrue(caixa.maxZ() <= desenhoMaisAvanco,
                "A caixa vai a " + caixa.maxZ() + " blocos e o focinho desenhado ("
                        + CheetahLeaderTuning.ALCANCE_DESENHADO_DA_FRENTE + ") mais o avanco ("
                        + CheetahLeaderTuning.AVANCO_DO_BOTE + ") chegam a " + desenhoMaisAvanco
                        + ". Caixa maior que o desenho da um jogador que apanha de um guepardo que,"
                        + " na tela, parou antes dele -- dano certo, cooldown certo, log limpo.");
        assertTrue(caixa.minZ() <= CheetahLeaderTuning.ALCANCE_DESENHADO_DA_FRENTE,
                "A caixa comeca a " + caixa.minZ() + " blocos, adiante da ponta do focinho: o bote"
                        + " sairia de um ponto na frente da propria boca.");
        // Esta e a outra ponta de um portao que morde dos dois lados. A primeira
        // esta em art-source/enemies/cheetah_leader/cheetah_leader_geo.py, em
        // valida_bote_alcanca_a_caixa: la o desenho e medido contra estes mesmos
        // numeros. Quem encurtar o focinho la reprova la; quem esticar a caixa
        // aqui reprova aqui.
    }

    @Test
    @DisplayName("a distancia de decisao cabe dentro do alcance da caixa")
    void decidirOBoteSoDentroDoQueEleAlcanca() {
        AttackHitbox caixa = CheetahLeaderTuning.caixaDoBote();
        double alcanceReal = caixa.maxZ() + CheetahLeaderTuning.MEIA_LARGURA_DE_UM_ALVO;
        assertTrue(CheetahLeaderTuning.ALCANCE_DO_BOTE < alcanceReal,
                "Decidir o bote a " + CheetahLeaderTuning.ALCANCE_DO_BOTE + " com a caixa"
                        + " alcancando " + alcanceReal + " faria ela comecar um windup contra"
                        + " alguem ja fora do alcance. Como ela trava a navegacao durante o golpe,"
                        + " o bote no limite NUNCA acertaria -- um lider que erra sozinho e parece"
                        + " quebrado.");
    }

    @Test
    @DisplayName("o arranque nao e autorizado dentro do alcance do bote")
    void oArranqueNaoCompeteComOGolpe() {
        assertTrue(CheetahLeaderTuning.DISTANCIA_MINIMA_DO_ARRANQUE
                        > CheetahLeaderTuning.ALCANCE_DO_BOTE,
                "Com a distancia minima do arranque dentro do alcance do bote, ela gastaria o ciclo"
                        + " inteiro para viajar zero bloco e ficaria FATIGADA colada no jogador: a"
                        + " vantagem do bicho viraria uma desvantagem, e nada acusaria.");
        assertTrue(CheetahLeaderTuning.DISTANCIA_DE_ABERTURA
                        <= CheetahLeaderTuning.DISTANCIA_MINIMA_DO_ARRANQUE,
                "O anel em que um companheiro ja conta como 'combate aberto' nao pode ser mais"
                        + " largo que a distancia em que ela arranca -- senao qualquer aliado"
                        + " parado ao lado dela cancelaria todo arranque, e a ficha desligaria em"
                        + " silencio.");
    }

    @Test
    @DisplayName("o telegrafo continua um telegrafo: o aviso e mais longo que a janela")
    void oAvisoENaoAJanelaEOQueSeLe() {
        AttackDefinition bote = CheetahLeaderTuning.bote();
        assertTrue(bote.windupTicks() > bote.activeTicks(),
                "Windup mais curto que a janela e um mob que bate sem aviso -- com o mesmo dano, o"
                        + " mesmo cooldown e o mesmo log limpo.");
        assertTrue(bote.recoveryTicks() > bote.windupTicks(),
                "A recuperacao e o PRECO de ter chegado primeiro. Menor que o aviso, chegar"
                        + " primeiro vira vantagem sem preco e o encontro nao tem fim.");
    }

    @Test
    @DisplayName("a janela que machuca nao e interrompivel, e a recuperacao tambem nao")
    void interrupcaoNaoCortaOGolpeJaVisto() {
        AttackDefinition bote = CheetahLeaderTuning.bote();
        assertTrue(bote.interruptibleWindup(),
                "O aviso TEM de ser interrompivel: e ele que faz cambalear valer a pena contra um"
                        + " bicho que so fica parado na recuperacao.");
        assertFalse(bote.interruptibleActive(),
                "Cancelar um golpe que o jogador ja viu sair quebra a unica leitura que um"
                        + " telegrafo de 0.6 s entrega.");
        assertFalse(bote.interruptibleRecovery(),
                "Com a recuperacao interrompivel, punir a lider a devolveria a fila de ataque mais"
                        + " rapido do que se ninguem tivesse encostado nela -- interromper passaria"
                        + " a ACELERAR o encontro.");
    }

    @Test
    @DisplayName("o dano do bote vem do atributo, e nao de um numero paralelo")
    void oDanoNaoTemDuasFontes() {
        assertEquals(DANO_DA_FICHA, CheetahLeaderTuning.bote().damage(),
                "Repetir o 14 aqui faria girar o atributo numa sessao de balanceamento mudar a"
                        + " barra de vida do jogador sem mudar este arquivo.");
        assertEquals(ChimeraProfiles.cheetahLeader().attributes().attackDamage(),
                CheetahLeaderTuning.bote().damage());
    }

    @Test
    @DisplayName("o esquadrao dela sai inteiro de SquadRules, sem numero redeclarado")
    void oEsquadraoNaoERedeclarado() {
        SquadRules regras = SquadRules.esquadrao();
        assertTrue(regras.maximoDeMembros() >= 2);
        assertTrue(regras.raioDeReforco() >= regras.espacamento());
        // A ponta de arte deste numero esta em cheetah_leader_geo.py, em
        // valida_corpo_cabe_no_espacamento: o corpo desenhado tem de caber duas
        // vezes no espacamento, senao "espacamento respeitado" poe formiga dentro
        // de formiga e o jogador le travamento, nunca IA.
        assertEquals(3.0D, regras.espacamento(),
                "O gerador de geometria copia este espacamento para medir o corpo desenhado. Mudou"
                        + " aqui e nao la, as duas contas passam a medir coisas diferentes -- e a"
                        + " divergencia so aparece na tela, como uma formacao empilhada.");
    }
}
