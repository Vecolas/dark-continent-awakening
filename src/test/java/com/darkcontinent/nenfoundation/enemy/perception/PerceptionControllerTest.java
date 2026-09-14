package com.darkcontinent.nenfoundation.enemy.perception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.api.EnemyAwarenessState;
import com.darkcontinent.nenfoundation.enemy.api.EnemyFaction;
import com.darkcontinent.nenfoundation.enemy.ai.AwarenessTuning;
import com.darkcontinent.nenfoundation.enemy.ai.EnemyBrain;
import com.darkcontinent.nenfoundation.enemy.faction.FactionRelations;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao do ORCAMENTO de percepcao (issue #136).
 *
 * <p>O caso que ele existe para reprovar nao levanta excecao nenhuma: um mob que
 * varre o mundo a cada tick funciona perfeitamente sozinho e derruba o TPS em
 * manada. Como nao ha erro para procurar, a unica defesa e CONTAR as varreduras e
 * reprovar o excesso.</p>
 */
class PerceptionControllerTest {

    private static final UUID JOGADOR = UUID.nameUUIDFromBytes("jogador".getBytes());
    private static final UUID OUTRO = UUID.nameUUIDFromBytes("outro".getBytes());

    private static PerceptionController controlador(SensorDeVisao sensor, int memoriaTicks) {
        return new PerceptionController(PerceptionBudget.padrao(),
                VisionCone.deGraus(24.0D, 70.0D), new ThreatMemory(memoriaTicks),
                new TargetEvaluator(FactionRelations.padrao(), EnemyFaction.WILDLIFE, 32.0D, true),
                PercepcaoDeAura.NENHUMA, 16.0D, 0);
    }

    private static TargetCandidate visivel(double distancia) {
        return new TargetCandidate(JOGADOR, EnemyFaction.CIVILIAN, distancia, 1.0D,
                true, true, true, false);
    }

    @Test
    @DisplayName("100 ticks nao produzem 100 varreduras -- o orcamento e cobrado, nao sugerido")
    void orcamentoLimitaVarreduras() {
        AtomicInteger chamadas = new AtomicInteger();
        SensorDeVisao sensor = () -> {
            chamadas.incrementAndGet();
            return List.of(visivel(6.0D));
        };
        PerceptionController controlador = controlador(sensor, 40);

        for (int tick = 0; tick < 100; tick++) {
            controlador.tick(tick, sensor, false, false);
        }

        assertEquals(chamadas.get(), controlador.varredurasFeitas(),
                "A regua tem de contar exatamente as chamadas ao sensor; se ela contar outra"
                        + " coisa, o portao passa a medir o vazio.");
        assertTrue(controlador.varredurasFeitas() <= 100 / PerceptionBudget.VISAO_MINIMA,
                "Varreduras demais em 100 ticks: " + controlador.varredurasFeitas()
                        + ". Isto NAO da erro em jogo -- da TPS caindo devagar ao longo de uma"
                        + " sessao, que e o relato de bug mais caro que existe.");
        assertTrue(controlador.varredurasFeitas() > 0,
                "Zero varreduras em 100 ticks: o mob ficou cego e o teste estaria aprovando"
                        + " economia total, que e pior do que gasto.");
    }

    @Test
    @DisplayName("entre duas varreduras o mob opera de memoria, e a memoria expira")
    void memoriaSustentaOAlvoEDepoisExpira() {
        PerceptionController controlador = controlador(List::of, 10);
        SensorDeVisao ve = () -> List.of(visivel(5.0D));
        SensorDeVisao cego = List::of;

        // tick 0 cai no multiplo do intervalo: e a varredura que enxerga.
        PerceptionSnapshot viu = controlador.tick(0, ve, false, false);
        assertTrue(viu.visivel());
        assertEquals(JOGADOR, viu.alvo());

        PerceptionSnapshot logoDepois = controlador.tick(1, cego, false, false);
        assertFalse(logoDepois.visivel(), "Fora da varredura ele nao 've' -- ele LEMBRA.");
        assertEquals(JOGADOR, logoDepois.alvo());
        assertFalse(logoDepois.paraCerebro().targetLost());

        for (int tick = 2; tick < 40; tick++) {
            controlador.tick(tick, cego, false, false);
        }
        assertEquals(null, controlador.ultimo().alvo(),
                "Memoria sem prazo transforma 'perdi de vista' em 'persigo para sempre'.");
        assertTrue(controlador.ultimo().paraCerebro().targetLost());
    }

    @Test
    @DisplayName("som de uma fonte desconhecida vira suspeita; som do alvo recarrega a memoria")
    void audicaoEEventoENaoVarredura() {
        PerceptionController controlador = controlador(List::of, 20);
        controlador.ouvir(new HearingEvent(OUTRO, 4.0D, 1.0D));
        PerceptionSnapshot snapshot = controlador.tick(1, List::of, false, false);

        assertTrue(snapshot.audivel());
        assertEquals(OUTRO, snapshot.alvo());
        assertFalse(snapshot.visivel(), "Ouvir nao e ver: o cerebro tem de ir INVESTIGAR.");

        EnemyBrain cerebro = new EnemyBrain(new AwarenessTuning(10, 20));
        assertEquals(EnemyAwarenessState.INVESTIGATE, cerebro.tick(snapshot.paraCerebro()));
    }

    @Test
    @DisplayName("som fraco e longe nao chega; som forte e perto chega")
    void alcanceDeAudicaoRespeitaIntensidade() {
        PerceptionController controlador = controlador(List::of, 20);
        controlador.ouvir(new HearingEvent(OUTRO, 15.0D, 0.1D));
        assertEquals(null, controlador.tick(1, List::of, false, false).alvo(),
                "Um passo agachado a 15 blocos nao pode acordar o mob.");

        controlador.ouvir(new HearingEvent(OUTRO, 15.0D, 1.0D));
        assertEquals(OUTRO, controlador.tick(2, List::of, false, false).alvo());
    }

    @Test
    @DisplayName("limpar apaga memoria, sons pendentes e ultimo snapshot JUNTOS")
    void limpezaESimetrica() {
        PerceptionController controlador = controlador(List::of, 20);
        controlador.tick(0, () -> List.of(visivel(3.0D)), false, false);
        assertTrue(controlador.alvoLembrado().isPresent());

        controlador.limpar();

        assertTrue(controlador.alvoLembrado().isEmpty());
        assertEquals(null, controlador.ultimo().alvo());
        assertTrue(controlador.ultimo().paraCerebro().targetLost());
    }

    @Test
    @DisplayName("a porta de aura nasce inerte: NENHUMA devolve a lista intacta")
    void portaDeAuraEInerte() {
        List<TargetCandidate> candidatos = List.of(visivel(2.0D));
        assertSame(candidatos, PercepcaoDeAura.NENHUMA.aplicar(candidatos),
                "Enquanto Gyo nao existir, a extensao nao pode custar nem uma copia de lista"
                        + " -- e nao pode inventar regra de Nen, que e autoridade do nucleo.");
    }

    @Test
    @DisplayName("orcamento fora da faixa da issue #136 reprova no construtor")
    void orcamentoForaDaFaixaReprova() {
        assertThrows(IllegalArgumentException.class, () -> new PerceptionBudget(1, 30),
                "Visao a cada tick e exatamente o que o orcamento existe para impedir.");
        assertThrows(IllegalArgumentException.class, () -> new PerceptionBudget(6, 5),
                "Scan 'caro' mais frequente que a visao deixa de ser caro.");
        assertThrows(IllegalArgumentException.class, () -> new PerceptionBudget(6, 100));
    }
}
