package com.darkcontinent.nenfoundation.enemy.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.ai.DecisaoDeExaustaoDeBolha;
import com.darkcontinent.nenfoundation.enemy.ai.DecisaoDeSaltoDeBolha;
import com.darkcontinent.nenfoundation.enemy.ai.RegrasDeExaustaoDeBolha;
import com.darkcontinent.nenfoundation.enemy.ai.RegrasDeSaltoDeBolha;
import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.greedisland.CaptureCondition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Os numeros que o Bubble Horse vai levar para o jogo, medidos contra a FICHA
 * dele -- e nao contra si mesmos.
 *
 * <p>{@code RegrasDeSaltoDeBolhaTest} e {@code RegrasDeExaustaoDeBolhaTest}
 * provam as REGRAS com numeros proprios. Este arquivo prova os VALORES DE
 * PRODUCAO: que a combinacao declarada em {@link BubbleHorseTuning} e
 * construivel, que o limiar da janela e o MESMO da condicao de card publicada, e
 * que o coice continua sendo o recurso de quem foi encurralado. Nenhuma dessas
 * coisas levanta erro quando quebra: o mod carrega, o cavalo nasce, e so falha o
 * que ele ensina.</p>
 */
class BubbleHorseTuningTest {

    /** O literal de GreedIslandProfiles.bubbleHorse(): new EnemyAttributes(30, 0.45F, 3, ...). */
    private static final float VIDA_MAXIMA = 30.0F;
    /** O alcance de ataque de um jogador em 1.21.1. Do jogo, nao deste bicho. */
    private static final double ALCANCE_DE_ATAQUE_DO_JOGADOR = 3.0D;

    @Test
    @DisplayName("os numeros declarados formam regras validas")
    void osNumerosDeProducaoSaoConstruiveis() {
        RegrasDeSaltoDeBolha salto = BubbleHorseTuning.salto();
        assertEquals(BubbleHorseTuning.TICKS_DE_PAUSA_ENTRE_SALTOS, salto.ticksDePausa());
        assertEquals(BubbleHorseTuning.DISTANCIA_DO_COICE, salto.distanciaDoCoice());
        // Os construtores ja cobram coice < conforto, pausa >= 1 e subida > 0.
        // Estas chamadas sao o que garante que a cobranca de fato roda sobre os
        // numeros de PRODUCAO, e nao so sobre os dos testes de regra.
        RegrasDeExaustaoDeBolha exaustao = BubbleHorseTuning.exaustao();
        assertEquals(BubbleHorseTuning.TICKS_DA_JANELA, exaustao.ticksDaJanela());
    }

    @Test
    @DisplayName("a janela abre no MESMO ponto em que o card passa a valer")
    void oLimiarDaJanelaEODaCondicaoDeCaptura() {
        CaptureCondition condicao = BubbleHorseTuning.condicaoDeCaptura();
        assertEquals(condicao.vidaMaximaFracao(),
                BubbleHorseTuning.exaustao().fracaoDeVidaDoColapso(),
                "dois limiares diferentes nao dao erro: o cavalo para num ponto de vida que nao"
                        + " paga card, o jogador para de bater e nao recebe nada -- e nao ha nada"
                        + " para procurar no log");
        assertTrue(condicao.exigeNaoLetal(),
                "com captura por abate a janela de exaustao vira um intervalo em que o bicho fica"
                        + " parado apanhando, e o unico sintoma e um mob que parece bugado");
    }

    @Test
    @DisplayName("um golpe de espada de ferro nao leva o cavalo de cheio a janela")
    void aJanelaNaoAbreNoPrimeiroGolpe() {
        float golpeDeEspadaDeFerro = 6.0F;
        assertEquals(DecisaoDeExaustaoDeBolha.ACIMA_DO_LIMIAR,
                BubbleHorseTuning.exaustao().decidir(VIDA_MAXIMA - golpeDeEspadaDeFerro,
                        VIDA_MAXIMA, false, false, 0, 0),
                "abrindo a janela no primeiro golpe, o jogador nunca chegaria a ver o cavalo FUGIR"
                        + " -- e a fuga e a metade da licao que sobra");
    }

    @Test
    @DisplayName("a janela e mais curta que o folego -- deixar passar tem custo")
    void deixarAJanelaPassarCusta() {
        assertTrue(BubbleHorseTuning.TICKS_DE_FOLEGO > BubbleHorseTuning.TICKS_DA_JANELA,
                "com o folego menor ou igual a janela, a segunda chance chegaria quase de graca e"
                        + " a primeira deixaria de importar -- sem que nada reprovasse");
    }

    @Test
    @DisplayName("ele decide coicear de uma distancia que a pata alcanca")
    void oCoiceNaoDecideDeLongeDemais() {
        double alcanceUtil = BubbleHorseTuning.CAIXA_DA_PATADA.maxZ()
                + BubbleHorseTuning.MEIA_LARGURA_DE_UM_ALVO;
        assertTrue(BubbleHorseTuning.DISTANCIA_DO_COICE < alcanceUtil,
                "decidindo de mais longe do que a caixa alcanca, o cavalo comeca uma empinada de "
                        + BubbleHorseTuning.WINDUP_DA_PATADA + " ticks contra alguem fora do"
                        + " alcance -- e como ele trava o corpo no golpe, o ataque no limite NUNCA"
                        + " acertaria. Isso da um chefe que erra sozinho, e nao um erro no log");
    }

    @Test
    @DisplayName("o jogador alcanca o cavalo antes de o cavalo alcancar o jogador")
    void oCoiceEMaisCurtoQueOSocoDoJogador() {
        assertTrue(BubbleHorseTuning.DISTANCIA_DO_COICE < ALCANCE_DE_ATAQUE_DO_JOGADOR,
                "com o coice alcancando mais que o golpe do jogador, encostar no bicho passaria a"
                        + " ser proibitivo e ninguem chegaria perto o bastante para descobrir a"
                        + " janela de captura -- que e a unica coisa que ele tem");
    }

    @Test
    @DisplayName("com saida livre ele NAO coiceia, nem colado")
    void oCoiceContinuaSendoORecursoDeQuemFoiEncurralado() {
        RegrasDeSaltoDeBolha salto = BubbleHorseTuning.salto();
        assertTrue(salto.coiceia(false, 0.8D, true));
        assertEquals(false, salto.coiceia(false, 0.8D, false),
                "o dano 3 e o coice de quem foi encurralado. Aprovado com saida livre, ele vira um"
                        + " ataque procurado e a ficha do bicho se inverte sem nada acusar");
        assertEquals(DecisaoDeSaltoDeBolha.SALTA,
                salto.decidir(false, false, false, true,
                        BubbleHorseTuning.TICKS_DE_PAUSA_ENTRE_SALTOS, 0.8D),
                "colado e com saida livre, a resposta dele e FUGIR");
    }

    @Test
    @DisplayName("ele percebe muito mais longe do que foge")
    void oConfortoEMenorQueOAlcanceDePercepcao() {
        double followRange = GreedIslandProfiles.bubbleHorse().attributes().followRange();
        assertTrue(BubbleHorseTuning.DISTANCIA_DE_CONFORTO < followRange,
                "igualando os dois, o cavalo saltaria no instante em que entrasse no alcance de"
                        + " percepcao, e ninguem chegaria perto o bastante para descobrir que ele"
                        + " vale card");
    }

    @Test
    @DisplayName("o orcamento da patada e o que a animacao copiou")
    void osTicksDaPatadaSaoOsDoArquivoDeAnimacao() {
        AttackDefinition patada = BubbleHorseTuning.patada(3.0F);
        // Estes tres numeros estao escritos tambem em
        // art-source/enemies/bubble_horse/bubble_horse_animacoes.py, no dicionario
        // ATAQUES, com o nome do campo ao lado. Duplicacao DECLARADA: a outra ponta
        // e o portao valida_duracao_de_ataque, que reprova quando os clipes ficam
        // mais curtos que o orcamento. Sem esta asercao, mexer aqui deixaria o
        // gerador de arte medindo um orcamento que o servidor nao usa mais.
        assertEquals(14, patada.windupTicks());
        assertEquals(4, patada.activeTicks());
        assertEquals(16, patada.recoveryTicks());
        assertTrue(patada.interruptibleWindup() && !patada.interruptibleActive(),
                "a empinada e interrompivel e a descida da pata nao: cancelar um golpe que o"
                        + " jogador ja viu sair apaga a unica leitura que um ataque telegrafado"
                        + " entrega");
    }

    @Test
    @DisplayName("o ciclo de salto e o que a animacao copiou")
    void oCicloDeSaltoEODoArquivoDeAnimacao() {
        // Copiado em bubble_horse_animacoes.py como TICKS_DE_PAUSA e TICKS_DE_ARCO,
        // e cobrado la por valida_ciclo_do_salto contra a duracao do clipe `walk`.
        assertEquals(12, BubbleHorseTuning.TICKS_DE_PAUSA_ENTRE_SALTOS);
        assertEquals(8, BubbleHorseTuning.TICKS_DE_ARCO_DO_SALTO);
        assertEquals(20, BubbleHorseTuning.salto().cicloEmTicks());
    }

    @Test
    @DisplayName("o dano da patada NAO e uma constante deste arquivo")
    void oDanoVemDoAtributoEAcompanhaOPerfil() {
        float doPerfil = GreedIslandProfiles.bubbleHorse().attributes().attackDamage();
        assertEquals(doPerfil, BubbleHorseTuning.patada(doPerfil).damage());
        assertNotEquals(BubbleHorseTuning.patada(9.0F).damage(),
                BubbleHorseTuning.patada(doPerfil).damage(),
                "a definicao tem de refletir o dano PASSADO, e nao um numero congelado aqui:"
                        + " congelado, todo buff, debuff e ajuste de perfil sumiria sem aviso");
    }

    // ------------------------------------------------- o caso que DEVE reprovar

    @Test
    @DisplayName("a regua de coerencia do coice reprova a inversao -- e reprova mesmo")
    void aInversaoDoCoiceEReprovada() {
        // ESTE E O CASO QUE DEVE REPROVAR, alimentado a mao. Ele existe porque um
        // teste que so confere os numeros de producao ficaria verde para sempre sem
        // nunca ter medido nada: a producao passa, e ninguem sabe se a regua morde.
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeSaltoDeBolha(BubbleHorseTuning.TICKS_DE_PAUSA_ENTRE_SALTOS,
                        BubbleHorseTuning.TICKS_DE_ARCO_DO_SALTO,
                        BubbleHorseTuning.IMPULSO_HORIZONTAL, BubbleHorseTuning.IMPULSO_VERTICAL,
                        BubbleHorseTuning.DISTANCIA_DO_COICE,
                        BubbleHorseTuning.DISTANCIA_DE_CONFORTO));
        assertTrue(erro.getMessage().contains("perseguidor de dano 3"),
                "a mensagem tem de dizer o que nasce da inversao; veio: " + erro.getMessage());
    }
}
