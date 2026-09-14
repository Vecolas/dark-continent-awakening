package com.darkcontinent.nenfoundation.enemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.ai.squad.Squad;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRegistry;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRole;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRules;
import com.darkcontinent.nenfoundation.enemy.combat.GrabController;
import com.darkcontinent.nenfoundation.enemy.combat.GrabRelease;
import com.darkcontinent.nenfoundation.enemy.combat.GrabRules;
import com.darkcontinent.nenfoundation.enemy.encounter.EncounterInstance;
import com.darkcontinent.nenfoundation.enemy.encounter.EncounterState;
import com.darkcontinent.nenfoundation.enemy.encounter.RewardLedger;
import com.darkcontinent.nenfoundation.enemy.greedisland.CardConversionService;
import com.darkcontinent.nenfoundation.enemy.greedisland.CardSpec;
import com.darkcontinent.nenfoundation.enemy.greedisland.DefeatResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Hardening de multiplayer (EN14 / issue #149) -- as CORRIDAS, e nao as threads.
 *
 * <p><b>A corrida que este projeto tem nao e de thread.</b> O tick de gameplay do
 * Minecraft e de uma linha so; dois jogadores que acertam o golpe final "ao mesmo
 * tempo" na verdade sao dois eventos ORDENADOS dentro do mesmo tick. Escrever
 * estruturas concorrentes aqui nao tornaria nada mais seguro -- sugeriria que e
 * legitimo chamar estes objetos de outra thread, e ai sim apareceria o defeito
 * de verdade, num lugar que ninguem olha.</p>
 *
 * <p>O que este arquivo cobra e a exclusao por ORDEM: a operacao que decide e
 * uma so, sem espaco entre consultar e gravar. E cobra tambem os pontos de saida
 * que a issue lista por nome -- disconnect durante agarrao, unload de lider,
 * participante que sai do encontro, e corrida de recompensa.</p>
 */
class HardeningMultiplayerTest {

    private static final UUID JOGADOR_A = UUID.nameUUIDFromBytes("a".getBytes());
    private static final UUID JOGADOR_B = UUID.nameUUIDFromBytes("b".getBytes());
    private static final ResourceLocation CYCLOPS =
            ResourceLocation.fromNamespaceAndPath("nenfoundation", "cyclops");

    private static EncounterInstance concluido() {
        EncounterInstance encontro = new EncounterInstance(UUID.randomUUID(), "gi:cyclops",
                Level.OVERWORLD, new BlockPos(0, 64, 0));
        encontro.estado(EncounterState.ARMED);
        encontro.estado(EncounterState.ACTIVE);
        encontro.estado(EncounterState.COMPLETED);
        return encontro;
    }

    // ------------------------------------------------ corrida de recompensa

    @Test
    @DisplayName("dez pedidos da MESMA recompensa no mesmo tick pagam UM")
    void corridaDeRecompensaPagaUmaVez() {
        RewardLedger ledger = new RewardLedger();
        UUID episodio = UUID.randomUUID();
        List<Boolean> respostas = new ArrayList<>();
        for (int i = 0; i < 10; i++) respostas.add(ledger.travar(episodio, "card"));

        assertEquals(1, respostas.stream().filter(Boolean::booleanValue).count(),
                "Entre um 'ja pagou?' e um 'entao pague' cabe o outro jogador. Uma operacao"
                        + " so nao tem esse meio, e e por isso que travar() decide e grava"
                        + " na mesma chamada.");
        assertTrue(respostas.get(0), "O PRIMEIRO pedido tem de ser o que paga.");
    }

    @Test
    @DisplayName("a corrida de card respeita o limite de copias do MUNDO")
    void corridaDeCardRespeitaEscassez() {
        RewardLedger ledger = new RewardLedger();
        CardConversionService servico = new CardConversionService(ledger,
                Map.of(CYCLOPS, new CardSpec(CYCLOPS, "A", 2)));

        int pagos = 0;
        for (int i = 0; i < 8; i++) {
            if (servico.converter(concluido(), CYCLOPS, DefeatResult.CAPTURADO).isPresent()) pagos++;
        }
        assertEquals(2, pagos,
                "Oito episodios concluidos disputando um card de duas copias tem de produzir"
                        + " DUAS. A tentativa recusada nao pode consumir copia, senao a escassez"
                        + " vira erosao e o card some do jogo sem ninguem ter recebido.");
        assertEquals(2, servico.emitidas(CYCLOPS));
    }

    @Test
    @DisplayName("a trava sobrevive a ida e volta do save -- e continua bloqueando")
    void travaAtravessaOSave() {
        RewardLedger antes = new RewardLedger();
        UUID episodio = UUID.randomUUID();
        assertTrue(antes.travar(episodio, "card"));

        RewardLedger depois = new RewardLedger();
        depois.carregar(antes.chaves());

        assertFalse(depois.travar(episodio, "card"),
                "A corrida que a trava impede e justamente a que SOBREVIVE ao restart: em"
                        + " memoria ela morre com o processo, e o dupe volta em todo restart"
                        + " com o item na mao de quem o recebeu.");
    }

    // -------------------------------------------------- disconnect no grab

    @Test
    @DisplayName("desconectar preso SOLTA o agarrao, e nao deixa o predador com um fantasma")
    void disconnectDuranteAgarraoSolta() {
        GrabController agarrao = new GrabController(new GrabRules(100, 20, 3.0F, 12.0F), 3.0D, 1.6D);
        agarrao.agarrar(JOGADOR_A);
        agarrao.tick(true, true, true);

        // O jogador saiu: o mundo diz que ele nao esta mais preso.
        GrabController.GrabTick resultado = agarrao.tick(true, false, true);
        assertEquals(GrabRelease.VITIMA_SUMIU, resultado.soltura());
        assertFalse(agarrao.agarrando(),
                "Sem esta saida o predador ficaria agarrando um fantasma: o relogio correria"
                        + " sozinho ate o fim e o proximo agarrao nunca aconteceria.");

        // E o predador volta a poder agarrar OUTRO jogador no mesmo tick seguinte.
        assertEquals(com.darkcontinent.nenfoundation.enemy.combat.GrabRefusal.NENHUMA,
                agarrao.podeAgarrar(1.8D, 0.6D, false, true));
    }

    @Test
    @DisplayName("dois jogadores nao ficam presos no mesmo predador")
    void umaBocaUmaVitima() {
        GrabController agarrao = new GrabController(new GrabRules(100, 20, 3.0F, 12.0F), 3.0D, 1.6D);
        agarrao.agarrar(JOGADOR_A);
        assertEquals(com.darkcontinent.nenfoundation.enemy.combat.GrabRefusal.JA_AGARRANDO,
                agarrao.podeAgarrar(1.8D, 0.6D, false, true));
        assertEquals(JOGADOR_A, agarrao.vitima().orElseThrow());
    }

    // ------------------------------------------------------ unload de lider

    @Test
    @DisplayName("unload do lider promove o proximo E mantem o indice do registro coerente")
    void unloadDoLiderNaoQuebraOIndice() {
        SquadRegistry registro = new SquadRegistry();
        UUID bando = UUID.randomUUID();
        UUID chefe = UUID.randomUUID();
        UUID segundo = UUID.randomUUID();

        registro.criar(bando, SquadRules.matilha(), chefe);
        registro.entrar(bando, segundo, SquadRole.FLANKER);

        registro.sair(chefe);

        Squad vivo = registro.bando(bando).orElseThrow();
        assertEquals(segundo, vivo.lider().orElseThrow(),
                "Matar ou descarregar o chefe tem de MUDAR a matilha, e nao some-la.");
        assertTrue(registro.bandoDe(segundo).isPresent());
        assertTrue(registro.bandoDe(chefe).isEmpty(),
                "O indice precisa soltar quem saiu: uma entrada morta faria o bicho ser"
                        + " recusado em todo bando futuro, para sempre e sem erro.");
    }

    @Test
    @DisplayName("bando esvaziado sai do mapa e nao ressuscita com o alvo antigo")
    void bandoEsvaziadoNaoRessuscita() {
        SquadRegistry registro = new SquadRegistry();
        UUID bando = UUID.randomUUID();
        UUID chefe = UUID.randomUUID();

        registro.criar(bando, SquadRules.matilha(), chefe);
        registro.bando(bando).orElseThrow().alvo(JOGADOR_A);
        registro.sair(chefe);
        assertEquals(1, registro.removerDissolvidos());

        registro.criar(bando, SquadRules.matilha(), chefe);
        assertTrue(registro.bando(bando).orElseThrow().alvo().isEmpty(),
                "Um bando novo que nasce com o alvo de um combate que acabou ha uma hora"
                        + " persegue alguem que ja saiu do servidor -- e nada acusa.");
    }

    // --------------------------------------------------- saida de encontro

    @Test
    @DisplayName("um participante sair nao encerra o encontro do outro")
    void saidaDeUmNaoDerrubaOOutro() {
        EncounterInstance encontro = new EncounterInstance(UUID.randomUUID(), "gi:cyclops",
                Level.OVERWORLD, BlockPos.ZERO);
        encontro.estado(EncounterState.ARMED);
        encontro.estado(EncounterState.ACTIVE);
        assertTrue(encontro.entrar(JOGADOR_A));
        assertTrue(encontro.entrar(JOGADOR_B));

        assertTrue(encontro.sair(JOGADOR_A));
        assertEquals(java.util.Set.of(JOGADOR_B), encontro.participantes(),
                "Quem ficou continua no episodio: o encontro so falha quando NAO sobra"
                        + " ninguem, e por tempo -- nao no instante em que o primeiro se"
                        + " desconecta.");
    }

    @Test
    @DisplayName("quem chega DEPOIS do fim nao entra, e nao ganha direito a recompensa")
    void oportunistaNaoEntraNoFim() {
        EncounterInstance encontro = concluido();
        assertFalse(encontro.entrar(JOGADOR_B),
                "Entrar depois do fim daria direito de recompensa a quem chegou para ver o"
                        + " cadaver.");
    }

    @Test
    @DisplayName("recompensa so sai de encontro CONCLUIDO, e o motivo aparece na recusa")
    void recompensaExigeConclusao() {
        EncounterInstance emCurso = new EncounterInstance(UUID.randomUUID(), "gi:cyclops",
                Level.OVERWORLD, BlockPos.ZERO);
        emCurso.estado(EncounterState.ARMED);
        emCurso.estado(EncounterState.ACTIVE);

        CardConversionService servico = new CardConversionService(new RewardLedger(),
                Map.of(CYCLOPS, new CardSpec(CYCLOPS, "A", 0)));
        IllegalStateException erro = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> servico.converter(emCurso, CYCLOPS, DefeatResult.CAPTURADO));
        assertTrue(erro.getMessage().contains("abandonou"),
                "Pagar antes do fim premiaria quem largou o combate no meio, e a recusa"
                        + " precisa dizer isso -- recusa sem motivo e o pior relato de bug.");
    }
}
