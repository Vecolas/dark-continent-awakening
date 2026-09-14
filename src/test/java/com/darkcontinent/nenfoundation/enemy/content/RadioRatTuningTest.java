package com.darkcontinent.nenfoundation.enemy.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Cobra as tres coisas do grito que, se mudarem, nao quebram nada visivel.
 *
 * <p>Um {@code AttackDefinition} com a flag errada compila, roda, anima, causa
 * dano e nao aparece em log nenhum. O que ele apaga e a resposta que o encounter
 * ensina, e isso nenhum portao ve -- por isso as flags sao cobradas aqui, com o
 * motivo escrito ao lado de cada uma.</p>
 */
class RadioRatTuningTest {

    private static final float DANO_DO_PERFIL =
            GreedIslandProfiles.radioRat().attributes().attackDamage();

    @Test
    @DisplayName("o telegrafo do grito e interrompivel: e a resposta que o bicho ensina")
    void oWindupContinuaInterrompivel() {
        AttackDefinition grito = RadioRatTuning.grito(DANO_DO_PERFIL);
        assertTrue(grito.interruptibleWindup(),
                "trocar esta flag por false nao quebra nada visivel: o rato continua gritando, o"
                        + " relatorio continua saindo e o log fica limpo. O que some e a unica"
                        + " resposta que o jogador tem -- calar o mensageiro antes do aviso");
    }

    @Test
    @DisplayName("o aviso dura mais que o grito, senao nao e aviso")
    void oTelegrafoEMaisLongoQueAJanelaAtiva() {
        AttackDefinition grito = RadioRatTuning.grito(DANO_DO_PERFIL);
        assertTrue(grito.windupTicks() > grito.activeTicks(),
                "windup " + grito.windupTicks() + " e active " + grito.activeTicks() + ": com o"
                        + " telegrafo menor que a janela ativa, o relatorio sai praticamente junto"
                        + " com o inicio da pose, e quem estava olhando nao tem o que fazer");
        assertTrue(grito.recoveryTicks() >= 1,
                "sem recuperacao o rato volta a gritar direto da propria animacao de grito, e"
                        + " quem chegou tarde perde a janela de punir");
        assertTrue(RadioRatTuning.TICKS_DE_OBSERVACAO >= 1,
                "a observacao e a PRIMEIRA janela, a que separa 'ele me viu' de 'ele contou'");
    }

    @Test
    @DisplayName("o dano do grito vem do perfil, e nao de uma constante paralela")
    void oDanoEntraPorParametro() {
        assertEquals(DANO_DO_PERFIL, RadioRatTuning.grito(DANO_DO_PERFIL).damage(), 1.0E-6F,
                "o dano tem de ser exatamente o que o perfil declara");
        // A regua que mede a regua: se grito() ignorasse o parametro e usasse uma
        // constante propria, o caso acima passaria por coincidencia no dia em que
        // os dois numeros fossem iguais -- e so nesse dia. Um valor diferente
        // prova que o numero ATRAVESSA, e nao que ele bate.
        assertEquals(9.5F, RadioRatTuning.grito(9.5F).damage(), 1.0E-6F,
                "o dano passado tem de atravessar: uma constante paralela aqui venceria a config"
                        + " em runtime e a sessao de balanceamento giraria um botao morto");
        assertNotEquals(DANO_DO_PERFIL, 9.5F,
                "este teste so prova alguma coisa enquanto os dois valores forem diferentes");
    }

    @Test
    @DisplayName("as regras publicadas pelo tuning sao aceitas pelo proprio contrato delas")
    void oTuningNaoPublicaNumeroQueOContratoRecusa() {
        // Construir ja valida: raio, intensidade e observacao passam pelo
        // construtor compacto de RadioRatReportRules. Um numero invalido escrito
        // no tuning estouraria no PRIMEIRO tick de um rato vivo, em producao, e
        // nao aqui.
        assertTrue(RadioRatTuning.relatorio().alcanca(0.0D),
                "o vizinho colado no rato tem de ser alcancado pelo grito");
        assertTrue(RadioRatTuning.INTENSIDADE_DO_RELATORIO <= 1.0D,
                "intensidade acima de 1 e o mesmo que dizer que o grito e mais alto que uma"
                        + " explosao, e o HearingEvent recusaria isso em runtime");
    }

    @Test
    @DisplayName("dano invalido reprova na definicao do golpe, e nao no meio do combate")
    void danoInvalidoReprova() {
        // ESTE E O CASO QUE DEVE REPROVAR: um atributo corrompido (NaN vindo de um
        // modificador de terceiro) viraria dano NaN, e dano NaN aplicado a um
        // jogador nao da erro -- da uma barra de vida que para de fazer sentido.
        assertThrows(IllegalArgumentException.class, () -> RadioRatTuning.grito(Float.NaN),
                "dano NaN tem de reprovar na construcao do ataque");
        assertThrows(IllegalArgumentException.class, () -> RadioRatTuning.grito(-1.0F),
                "dano negativo curaria quem levou a mordida, sem uma linha de log");
    }
}
