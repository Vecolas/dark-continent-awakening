package com.darkcontinent.nenfoundation.enemy.encounter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao da fundacao de encontro (issue #141).
 *
 * <p>As falhas que ele cobre sao todas de PERSISTENCIA, e nenhuma levanta
 * excecao: recompensa paga duas vezes depois de um restart, encontro que volta de
 * concluido para ativo, episodio que herda participantes da rodada anterior, e
 * trava que expira sozinha. O gate da issue -- "reiniciar o servidor durante um
 * encontro sem duplicar entidade nem recompensa" -- e servidor de pe; o que este
 * arquivo prova sao as REGRAS que aquele gate vai exercitar.</p>
 */
class EncounterFoundationTest {

    private static final UUID EPISODIO = UUID.nameUUIDFromBytes("episodio".getBytes());
    private static final UUID OUTRO_EPISODIO = UUID.nameUUIDFromBytes("outro".getBytes());
    private static final UUID JOGADOR = UUID.nameUUIDFromBytes("jogador".getBytes());
    private static final UUID SEGUNDO = UUID.nameUUIDFromBytes("segundo".getBytes());

    private static EncounterInstance instancia() {
        return new EncounterInstance(EPISODIO, "nenfoundation:swamp_master",
                Level.OVERWORLD, new BlockPos(10, 64, -20));
    }

    // ------------------------------------------------------------ transicoes

    @Test
    @DisplayName("COMPLETED nunca volta para ACTIVE -- a recompensa sairia duas vezes")
    void concluidoNaoVoltaParaAtivo() {
        assertFalse(EncounterTransitions.permitida(EncounterState.COMPLETED, EncounterState.ACTIVE));
        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> EncounterTransitions.exigir(EncounterState.COMPLETED, EncounterState.ACTIVE));
        assertTrue(erro.getMessage().contains("duas vezes"),
                "A recusa tem de dizer o que aconteceria; sem isso ela e so um 'nao'.");
    }

    @Test
    @DisplayName("o caminho de volta passa OBRIGATORIAMENTE por um estado sem entidade")
    void repetirExigePassarPeloVazio() {
        EncounterInstance encontro = instancia();
        encontro.estado(EncounterState.ARMED);
        encontro.estado(EncounterState.ACTIVE);
        encontro.registrarEntidade(UUID.randomUUID());
        encontro.entrar(JOGADOR);
        encontro.estado(EncounterState.COMPLETED);

        assertThrows(IllegalStateException.class, () -> encontro.estado(EncounterState.ARMED),
                "COMPLETED -> ARMED direto herdaria entidades e participantes da rodada"
                        + " anterior, e o primeiro tick da segunda a concluiria sozinha.");

        encontro.estado(EncounterState.COOLDOWN);
        encontro.estado(EncounterState.ARMED);
        assertTrue(encontro.entidades().isEmpty(), "Rearmar tem de limpar o episodio inteiro.");
        assertTrue(encontro.participantes().isEmpty());
    }

    @Test
    @DisplayName("trocar de estado ZERA o relogio do estado")
    void relogioZeraNaTroca() {
        EncounterInstance encontro = instancia();
        encontro.estado(EncounterState.ARMED);
        for (int tick = 0; tick < 50; tick++) encontro.tick();
        assertEquals(50, encontro.ticksNoEstado());
        encontro.estado(EncounterState.ACTIVE);
        assertEquals(0, encontro.ticksNoEstado(),
                "Herdar o tempo faria a recarga acabar antes de comecar, e o encontro"
                        + " reapareceria rapido demais sem nenhum erro.");
    }

    @Test
    @DisplayName("encontro terminado nao aceita participante novo")
    void terminadoNaoAceitaParticipante() {
        EncounterInstance encontro = instancia();
        encontro.estado(EncounterState.ARMED);
        encontro.estado(EncounterState.ACTIVE);
        encontro.estado(EncounterState.COMPLETED);
        assertFalse(encontro.entrar(JOGADOR),
                "Entrar depois do fim daria direito de recompensa a quem chegou no final.");
    }

    @Test
    @DisplayName("encontro sem definitionId reprova no construtor")
    void definicaoVaziaReprova() {
        assertThrows(IllegalArgumentException.class, () -> new EncounterInstance(
                EPISODIO, "  ", Level.OVERWORLD, BlockPos.ZERO));
        assertThrows(NullPointerException.class, () -> new EncounterInstance(
                EPISODIO, "x", null, BlockPos.ZERO));
    }

    // --------------------------------------------------------------- ledger

    @Test
    @DisplayName("a mesma recompensa do mesmo episodio so paga UMA vez")
    void recompensaPagaUmaVez() {
        RewardLedger ledger = new RewardLedger();
        assertTrue(ledger.travar(EPISODIO, "card"), "A primeira chamada e a que paga.");
        assertFalse(ledger.travar(EPISODIO, "card"),
                "A corrida de dois jogadores no mesmo tick cabe entre um 'consultar' e um"
                        + " 'gravar'. Uma operacao so nao tem esse meio.");
        assertTrue(ledger.jaPago(EPISODIO, "card"));
    }

    @Test
    @DisplayName("a trava e por EPISODIO, nao por recompensa nem por encontro")
    void chaveEOPar() {
        RewardLedger ledger = new RewardLedger();
        ledger.travar(EPISODIO, "card");
        assertTrue(ledger.travar(OUTRO_EPISODIO, "card"),
                "Travar so por rewardId faria o card cair uma vez na vida do mundo.");
        assertTrue(ledger.travar(EPISODIO, "xp"),
                "Travar so por encounterId impediria o episodio de pagar duas recompensas"
                        + " diferentes.");
        assertEquals(3, ledger.tamanho());
    }

    @Test
    @DisplayName("so o reset administrativo destrava; nada expira sozinho")
    void apenasResetDestrava() {
        RewardLedger ledger = new RewardLedger();
        ledger.travar(EPISODIO, "card");
        ledger.travar(EPISODIO, "xp");
        ledger.travar(OUTRO_EPISODIO, "card");

        assertEquals(2, ledger.limparEncontro(EPISODIO));
        assertFalse(ledger.jaPago(EPISODIO, "card"));
        assertTrue(ledger.jaPago(OUTRO_EPISODIO, "card"),
                "Limpar um episodio nao pode derrubar a trava de outro.");
    }

    @Test
    @DisplayName("recompensa sem id reprova -- a chave viraria generica e casaria com tudo")
    void recompensaSemIdReprova() {
        RewardLedger ledger = new RewardLedger();
        assertThrows(IllegalArgumentException.class, () -> ledger.travar(EPISODIO, ""));
        assertThrows(NullPointerException.class, () -> ledger.travar(null, "card"));
    }

    @Test
    @DisplayName("trava malformada vinda do save reprova em vez de virar chave morta")
    void travaMalformadaNoSaveReprova() {
        RewardLedger ledger = new RewardLedger();
        assertThrows(IllegalArgumentException.class, () -> ledger.carregar(List.of("sem_barra")));
        assertThrows(IllegalArgumentException.class, () -> ledger.carregar(List.of("/so_a_barra")));
        ledger.carregar(List.of(EPISODIO + "/card"));
        assertTrue(ledger.jaPago(EPISODIO, "card"));
    }

    @Test
    @DisplayName("carregar SUBSTITUI o conteudo, nunca soma")
    void carregarSubstitui() {
        RewardLedger ledger = new RewardLedger();
        ledger.travar(EPISODIO, "antigo");
        ledger.carregar(Set.of(OUTRO_EPISODIO + "/novo"));
        assertFalse(ledger.jaPago(EPISODIO, "antigo"),
                "Somar faria o save carregado herdar travas da sessao anterior do processo,"
                        + " e duas partidas no mesmo servidor divergiriam em silencio.");
        assertEquals(1, ledger.tamanho());
    }

    // ---------------------------------------------------------------- regras

    @Test
    @DisplayName("raio de abandono precisa de folga sobre o de ativacao")
    void histereseECobrada() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new EncounterRules(24.0D, 25.0D, 600, 0));
        assertTrue(erro.getMessage().contains("folga"),
                "Sem histerese o encontro liga e desliga a cada passo na borda, e o jogador"
                        + " le isso como bug de rede.");
        assertTrue(EncounterRules.campo().repetivel());
        assertFalse(EncounterRules.unico().repetivel(),
                "Cooldown zero quer dizer 'nao volta', e e assim que um encontro unico se"
                        + " declara -- nao apagando o encontro depois.");
    }

    @Test
    @DisplayName("regras impossiveis reprovam")
    void regrasImpossiveisReprovam() {
        assertThrows(IllegalArgumentException.class, () -> new EncounterRules(0, 10, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new EncounterRules(10, 30, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new EncounterRules(10, 30, 1, -1));
    }

    // --------------------------------------------------------- participantes

    @Test
    @DisplayName("dois participantes convivem; sair de um nao derruba o outro")
    void participantesSaoIndependentes() {
        EncounterInstance encontro = instancia();
        encontro.estado(EncounterState.ARMED);
        encontro.estado(EncounterState.ACTIVE);
        assertTrue(encontro.entrar(JOGADOR));
        assertTrue(encontro.entrar(SEGUNDO));
        assertFalse(encontro.entrar(JOGADOR), "Entrar duas vezes nao pode contar duas vezes.");
        assertTrue(encontro.sair(JOGADOR));
        assertEquals(Set.of(SEGUNDO), encontro.participantes());
    }
}
