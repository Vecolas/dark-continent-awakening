package com.darkcontinent.nenfoundation.enemy.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHitbox;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Cobra as coisas do batedor que, se mudarem, nao quebram nada visivel.
 *
 * <p>Um {@code AttackDefinition} com a flag errada compila, roda, anima, causa
 * dano e nao aparece em log nenhum. O que ele apaga e a resposta que o encontro
 * ensina, e isso nenhum portao ve -- por isso as flags sao cobradas aqui, com o
 * motivo escrito ao lado de cada uma.</p>
 */
class BatScoutTuningTest {

    private static final float DANO_DO_PERFIL =
            ChimeraProfiles.batScout().attributes().attackDamage();

    @Test
    @DisplayName("as tres janelas da mordida sao interrompiveis: e a recompensa de alcanca-lo")
    void aMordidaContinuaInterrompivel() {
        AttackDefinition mordida = BatScoutTuning.mordida(DANO_DO_PERFIL);
        assertTrue(mordida.interruptibleWindup() && mordida.interruptibleActive()
                        && mordida.interruptibleRecovery(),
                "trocar qualquer uma por false nao quebra nada visivel: ele continua mordendo, o"
                        + " dano continua saindo e o log fica limpo. O que some e a recompensa de"
                        + " ter chegado perto de um bicho que passa a vida fora de alcance");
    }

    @Test
    @DisplayName("o aviso do golpe dura mais que a janela que machuca")
    void oTelegrafoEhMaisLongoQueAJanelaAtiva() {
        AttackDefinition mordida = BatScoutTuning.mordida(DANO_DO_PERFIL);
        assertTrue(mordida.windupTicks() > mordida.activeTicks(),
                "windup " + mordida.windupTicks() + " e active " + mordida.activeTicks() + ": com"
                        + " o telegrafo menor que a janela ativa, a mordida sai junto com o inicio"
                        + " da pose e quem estava olhando nao tem o que fazer");
        assertTrue(mordida.recoveryTicks() >= 1,
                "sem recuperacao ele volta a morder direto da propria animacao de mordida, e quem"
                        + " chegou tarde perde a janela de punir");
        assertTrue(BatScoutTuning.TICKS_DE_OBSERVACAO >= 1,
                "a observacao e a PRIMEIRA janela, a que separa 'ele me viu' de 'ele contou'");
        assertTrue(BatScoutTuning.INTERVALO_ENTRE_RELATORIOS > BatScoutTuning.TICKS_DE_OBSERVACAO,
                "o intervalo entre relatorios tem de ser maior que a observacao: igual ou menor,"
                        + " o batedor relataria de novo antes de terminar de observar, e o custo"
                        + " da varredura passaria a ser continuo");
    }

    @Test
    @DisplayName("o dano da mordida vem do perfil, e nao de uma constante paralela")
    void oDanoEntraPorParametro() {
        assertEquals(DANO_DO_PERFIL, BatScoutTuning.mordida(DANO_DO_PERFIL).damage(), 1.0E-6F,
                "o dano tem de ser exatamente o que o perfil declara");
        // A regua que mede a regua: se mordida() ignorasse o parametro e usasse uma
        // constante propria, o caso acima passaria por coincidencia no dia em que os
        // dois numeros fossem iguais -- e so nesse dia.
        assertEquals(7.5F, BatScoutTuning.mordida(7.5F).damage(), 1.0E-6F,
                "o dano passado tem de ATRAVESSAR: uma constante paralela aqui venceria a config"
                        + " em runtime e a sessao de balanceamento giraria um botao morto");
        assertNotEquals(DANO_DO_PERFIL, 7.5F,
                "este teste so prova alguma coisa enquanto os dois valores forem diferentes");
    }

    @Test
    @DisplayName("a caixa da mordida aponta para a FRENTE do mundo, e nao para a da geometria")
    void aCaixaDaMordidaApontaParaFrente() {
        AttackHitbox caixa = BatScoutTuning.caixaDaMordida();
        assertTrue(caixa.minZ() >= 0.0D && caixa.maxZ() > 0.0D,
                "a caixa vive em Z POSITIVO. -Z e a frente da GEOMETRIA do modelo; esta caixa"
                        + " passa por AttackHitbox.noMundo, onde yaw 0 aponta para +Z. Trocar os"
                        + " dois poe a mordida ATRAS do morcego: ele morde, anima, e acerta quem"
                        + " estiver pelas costas, sem erro nenhum");
        assertEquals(BatScoutTuning.ALCANCE_DA_MORDIDA, caixa.maxZ(), 1.0E-9D,
                "o alcance da caixa e o mesmo numero que a arte cobra contra o focinho desenhado"
                        + " mais a arremetida do clipe; duas fontes aqui fariam o gerador medir"
                        + " uma coisa e o servidor entregar outra");
    }

    @Test
    @DisplayName("as regras publicadas pelo tuning sao aceitas pelo proprio contrato delas")
    void oTuningNaoPublicaNumeroQueOContratoRecusa() {
        // Construir ja valida: os construtores compactos das tres regras cobram
        // faixa, ordem e coerencia. Um numero invalido escrito aqui estouraria no
        // PRIMEIRO tick de um batedor vivo, em producao, e nao neste teste.
        assertTrue(BatScoutTuning.relatorio().alcanca(0.0D),
                "o vizinho colado no batedor tem de ser alcancado pelo aviso");
        assertTrue(BatScoutTuning.voo().recuoAoLevarDano(true, false) > 0.0D,
                "um batedor que apanha e nao sobe le como mob de chao fraco");
        assertTrue(BatScoutTuning.visaoNoturna().alcanceEfetivo(32.0D, 0, true) == 32.0D,
                "no escuro total, com o trait, o alcance e o mesmo -- e isso e o bicho");
        assertTrue(BatScoutTuning.MAXIMO_DE_VIZINHOS_POR_AVISO > 0,
                "teto zero de vizinhos faria o aviso nunca chegar a ninguem, e o batedor"
                        + " continuaria gastando a varredura para nada");
    }

    @Test
    @DisplayName("dano invalido reprova na definicao do golpe, e nao no meio do combate")
    void danoInvalidoReprova() {
        // ESTE E O CASO QUE DEVE REPROVAR: um atributo corrompido (NaN vindo de um
        // modificador de terceiro) viraria dano NaN, e dano NaN aplicado a um jogador
        // nao da erro -- da uma barra de vida que para de fazer sentido.
        assertThrows(IllegalArgumentException.class, () -> BatScoutTuning.mordida(Float.NaN),
                "dano NaN tem de reprovar na construcao do ataque");
        assertThrows(IllegalArgumentException.class, () -> BatScoutTuning.mordida(-1.0F),
                "dano negativo curaria quem levou a mordida, sem uma linha de log");
    }
}
