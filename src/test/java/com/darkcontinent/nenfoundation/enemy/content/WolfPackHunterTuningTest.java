package com.darkcontinent.nenfoundation.enemy.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.ai.RegrasDeMatilha;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRules;
import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHitbox;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Os numeros que o Wolf Pack Hunter vai levar para o jogo, medidos contra a ficha
 * dele -- e contra o DESENHO.
 *
 * <p>{@code RegrasDeMatilhaTest} prova a REGRA com numeros proprios. Este arquivo
 * prova os VALORES DE PRODUCAO: que a combinacao declarada em
 * {@link WolfPackHunterTuning} e construivel, que a caixa de mordida cabe no
 * focinho que foi desenhado, e que o telegrafo continua sendo um telegrafo.
 * Nenhuma dessas coisas levanta erro quando quebra -- o mod carrega, o lobo
 * nasce, e so falha o que ele ensina.</p>
 */
class WolfPackHunterTuningTest {

    /** O literal de GreedIslandProfiles.wolfPackHunter(): new EnemyAttributes(26, 0.34F, 6, ...). */
    private static final float DANO_DA_FICHA = 6.0F;

    @Test
    @DisplayName("os numeros declarados formam regras de matilha validas")
    void osNumerosDeProducaoSaoConstruiveis() {
        RegrasDeMatilha regras = WolfPackHunterTuning.matilha();
        assertEquals(WolfPackHunterTuning.MEMBROS_PARA_AVANCAR, regras.membrosParaAvancar());
        assertEquals(WolfPackHunterTuning.RAIO_DO_CERCO, regras.raioDoCerco());
        assertEquals(WolfPackHunterTuning.ALCANCE_DA_MORDIDA, regras.distanciaDeInvestida());
        // O construtor ja cobra o anel, o teto e a escada de papeis. Esta chamada
        // e o que garante que a cobranca de fato roda sobre os numeros de
        // PRODUCAO, e nao so sobre os do teste da regra.
        assertEquals(SquadRules.matilha(), regras.bando());
    }

    @Test
    @DisplayName("a caixa de mordida cabe no focinho desenhado mais o salto")
    void aMordidaNaoAlcancaAlemDoDesenho() {
        AttackHitbox caixa = WolfPackHunterTuning.caixaDaMordida();
        double desenhoMaisSalto = WolfPackHunterTuning.ALCANCE_DESENHADO_DO_FOCINHO
                + WolfPackHunterTuning.AVANCO_DA_INVESTIDA;
        assertTrue(caixa.maxZ() <= desenhoMaisSalto,
                "A caixa vai a " + caixa.maxZ() + " blocos e o focinho desenhado ("
                        + WolfPackHunterTuning.ALCANCE_DESENHADO_DO_FOCINHO + ") mais o salto ("
                        + WolfPackHunterTuning.AVANCO_DA_INVESTIDA + ") chegam a " + desenhoMaisSalto
                        + ". Caixa maior que o desenho da um jogador que apanha de um lobo que, na"
                        + " tela, parou antes dele -- dano certo, cooldown certo, log limpo.");
        assertTrue(caixa.minZ() <= WolfPackHunterTuning.ALCANCE_DESENHADO_DO_FOCINHO,
                "A caixa comeca a " + caixa.minZ() + " blocos, adiante da ponta do focinho: a"
                        + " mordida sairia de um ponto na frente da propria boca.");
        // Esta e a outra ponta de um portao que morde dos dois lados. A primeira
        // esta em art-source/enemies/wolf_pack_hunter/wolf_pack_hunter_geo.py,
        // em valida_focinho_alcanca_a_mordida: la o desenho e medido contra estes
        // mesmos numeros. Quem encolher o focinho reprova la; quem esticar a
        // caixa reprova aqui.
    }

    @Test
    @DisplayName("a distancia de decisao cabe dentro do alcance da caixa")
    void decidirMorderSoDentroDoQueAMordidaAlcanca() {
        AttackHitbox caixa = WolfPackHunterTuning.caixaDaMordida();
        double alcanceReal = caixa.maxZ() + WolfPackHunterTuning.MEIA_LARGURA_DE_UM_ALVO;
        assertTrue(WolfPackHunterTuning.ALCANCE_DA_MORDIDA < alcanceReal,
                "Decidir morder a " + WolfPackHunterTuning.ALCANCE_DA_MORDIDA + " com a caixa"
                        + " alcancando " + alcanceReal + " faria o lobo comecar um windup contra"
                        + " alguem ja fora do alcance. Como ele trava a navegacao durante o golpe,"
                        + " a mordida no limite NUNCA acertaria -- um bando que erra sozinho e"
                        + " parece quebrado.");
    }

    @Test
    @DisplayName("o anel fica FORA do alcance da mordida")
    void oAnelNaoEncostaNoAlvo() {
        assertTrue(WolfPackHunterTuning.RAIO_DO_CERCO > WolfPackHunterTuning.ALCANCE_DA_MORDIDA,
                "Iguais, 'segurar o anel' e 'morder' acontecem no mesmo lugar e os quatro lobos"
                        + " colam na cara do alvo -- a pilha que o espacamento existe para impedir.");
    }

    @Test
    @DisplayName("o telegrafo continua um telegrafo: o aviso e mais longo que a janela")
    void oAvisoENaoAJanelaEOQueSeLe() {
        AttackDefinition mordida = WolfPackHunterTuning.mordida();
        assertTrue(mordida.windupTicks() > mordida.activeTicks(),
                "Windup mais curto que a janela e um mob que bate sem aviso -- com o mesmo dano, o"
                        + " mesmo cooldown e o mesmo log limpo.");
        assertTrue(mordida.recoveryTicks() > mordida.activeTicks(),
                "A recuperacao e o que ESPACA as mordidas de dois investidores no tempo. Curta, o"
                        + " cerco vira trituradora.");
    }

    @Test
    @DisplayName("a janela que machuca nao e interrompivel, e a recuperacao tambem nao")
    void interrupcaoNaoCortaOGolpeJaVisto() {
        AttackDefinition mordida = WolfPackHunterTuning.mordida();
        assertTrue(mordida.interruptibleWindup(),
                "O aviso TEM de ser interrompivel: e ele que faz cambalear valer a pena.");
        assertFalse(mordida.interruptibleActive(),
                "Cancelar um golpe que o jogador ja viu sair quebra a unica leitura que um"
                        + " telegrafo de meio segundo entrega.");
        assertFalse(mordida.interruptibleRecovery(),
                "Com a recuperacao interrompivel, punir um membro o devolveria a fila de ataque"
                        + " antes dos irmaos que nao apanharam -- e punir aceleraria o cerco.");
    }

    @Test
    @DisplayName("o dano da mordida vem do atributo, e nao de um numero paralelo")
    void oDanoNaoTemDuasFontes() {
        assertEquals(DANO_DA_FICHA, WolfPackHunterTuning.mordida().damage(),
                "Repetir o 6 aqui faria girar o atributo numa sessao de balanceamento mudar a barra"
                        + " de vida do jogador sem mudar este arquivo.");
        assertEquals(GreedIslandProfiles.wolfPackHunter().attributes().attackDamage(),
                WolfPackHunterTuning.mordida().damage());
    }

    @Test
    @DisplayName("a recarga apos interrupcao e mais longa que a recarga normal")
    void interromperTemDeValer() {
        assertTrue(WolfPackHunterTuning.RECARGA_APOS_INTERRUPCAO
                        > GreedIslandProfiles.wolfPackHunterRecarga(),
                "Sem isso, quem interrompe ganha um ataque imediato na cara e o jogador aprende a"
                        + " NAO interromper -- o oposto do que o cambaleio existe para ensinar.");
    }

    @Test
    @DisplayName("o orcamento da mordida e o que a animacao copiou")
    void osTicksSaoOsDoArquivoDeAnimacao() {
        AttackDefinition mordida = WolfPackHunterTuning.mordida();
        // Estes tres numeros estao escritos tambem em
        // art-source/enemies/wolf_pack_hunter/wolf_pack_hunter_animacoes.py, no
        // dicionario ATAQUES, com o nome do campo ao lado. Duplicacao DECLARADA: a
        // outra ponta e o portao valida_duracao_de_ataque, que reprova quando os
        // clipes ficam mais curtos que o orcamento. Sem esta asercao, mexer aqui
        // deixaria o gerador de arte medindo um orcamento que o servidor nao usa.
        assertEquals(10, mordida.windupTicks());
        assertEquals(4, mordida.activeTicks());
        assertEquals(8, mordida.recoveryTicks());
    }

    @Test
    @DisplayName("a caixa de mordida e estreita: uma boca nao varre")
    void aMordidaNaoVarreOCirculo() {
        AttackHitbox caixa = WolfPackHunterTuning.caixaDaMordida();
        double largura = caixa.maxX() - caixa.minX();
        assertTrue(largura <= 1.0D,
                "Caixa larga (" + largura + ") num bando de quatro significa quatro varreduras"
                        + " cobrindo o circulo inteiro, e sair de perto deixa de ser resposta.");
        assertEquals(0.0D, caixa.minY(),
                "A mordida sai na altura do chao porque o lobo tem 0.9 bloco: comecar acima disso"
                        + " faria o golpe passar por cima de quem agacha, sem nada acusar.");
    }
}
