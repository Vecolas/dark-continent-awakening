package com.darkcontinent.nenfoundation.enemy.chimera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao da colonia persistente e da recuperacao offline (issue #144).
 *
 * <p>A falha central e a mais tentadora do sistema: "o jogador ficou tres dias
 * fora, entao a colonia cresceu tres dias". Implementada literalmente, ela
 * materializa centenas de entidades no instante em que o chunk carrega -- e nao
 * aparece como erro, porque cada formiga e legitima e o calculo esta "certo".</p>
 */
class ChimeraColonyTest {

    private static final UUID ID = UUID.nameUUIDFromBytes("colonia".getBytes());
    private static final UUID MEMBRO = UUID.nameUUIDFromBytes("membro".getBytes());
    private static final UUID OUTRO = UUID.nameUUIDFromBytes("outro".getBytes());
    private static final UUID AMEACA = UUID.nameUUIDFromBytes("ameaca".getBytes());
    private static final BlockPos NINHO = new BlockPos(120, 40, -80);

    private static ChimeraColony colonia() {
        return new ChimeraColony(ID, NINHO);
    }

    // ------------------------------------------------------- offline

    @Test
    @DisplayName("uma ausencia de um mes rende o MESMO que o teto -- nunca um exercito")
    void ausenciaLongaRendeOTeto() {
        SimulacaoOffline simulacao = SimulacaoOffline.padrao();
        int umDia = simulacao.ciclos(24_000L);
        int umMes = simulacao.ciclos(24_000L * 30L);
        assertEquals(umDia, umMes,
                "Sem teto, tres dias fora viram centenas de entidades no instante em que o"
                        + " chunk carrega -- e nada acusa, porque cada formiga e legitima.");
        assertTrue(umDia > 0, "O teto nao pode zerar a recuperacao: a colonia congelaria.");
    }

    @Test
    @DisplayName("ausencia curta rende PROPORCIONALMENTE -- o teto e de tempo, nao de resultado")
    void ausenciaCurtaRendeProporcional() {
        SimulacaoOffline simulacao = SimulacaoOffline.padrao();
        assertEquals(1, simulacao.ciclos(6_000L));
        assertEquals(2, simulacao.ciclos(12_000L),
                "Limitar o RESULTADO faria um dia e um mes renderem igual, e o tempo perderia"
                        + " o sentido.");
        assertEquals(0, simulacao.ciclos(5_999L));
    }

    @Test
    @DisplayName("o orcamento corta os nascimentos, e corta A CADA um")
    void orcamentoCortaNascimentos() {
        SimulacaoOffline simulacao = new SimulacaoOffline(24_000, 1_000, 2, 1);
        ChimeraTrackingBudget orcamento = new ChimeraTrackingBudget(10, 4, 12);

        var resultado = simulacao.recuperar(24_000L, orcamento, 8, 0);
        assertEquals(2, resultado.nascimentosAutorizados(),
                "Uma checagem unica autorizaria a leva inteira contra a contagem ANTIGA, e o"
                        + " teto seria estourado de uma vez -- o pico que ele existe para"
                        + " impedir.");
        assertTrue(resultado.comida() > 0);
    }

    @Test
    @DisplayName("colonia cheia nao autoriza nascimento nenhum")
    void coloniaCheiaNaoCresce() {
        SimulacaoOffline simulacao = SimulacaoOffline.padrao();
        var resultado = simulacao.recuperar(24_000L, new ChimeraTrackingBudget(10, 4, 12), 10, 2);
        assertEquals(0, resultado.nascimentosAutorizados());
    }

    @Test
    @DisplayName("relogio andando para tras reprova em vez de virar colonia que encolhe")
    void relogioParaTrasReprova() {
        assertThrows(IllegalArgumentException.class,
                () -> SimulacaoOffline.padrao().ciclos(-1L));
    }

    @Test
    @DisplayName("teto menor que um ciclo reprova -- nenhuma ausencia renderia nada")
    void tetoMenorQueUmCicloReprova() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new SimulacaoOffline(100, 6_000, 4, 1));
        assertTrue(erro.getMessage().contains("congelaria"));
    }

    @Test
    @DisplayName("a simulacao devolve NUMEROS e nunca cria nada")
    void simulacaoNaoCriaEntidade() {
        ChimeraColony colonia = colonia();
        colonia.marcarTick(0L);
        var resultado = colonia.recuperarAusencia(SimulacaoOffline.padrao(), 24_000L,
                ChimeraTrackingBudget.padrao(), 0, 0);
        assertTrue(resultado.rendeuAlgo());
        assertEquals(0, colonia.tamanho(),
                "A colonia so atualiza o proprio placar; quem materializa e o lado que tem"
                        + " mundo, e so quando o orcamento permitir.");
        assertEquals(4, resultado.nascimentosAutorizados());
        assertEquals(4, colonia.nascimentosPendentes(),
                "A autorizacao precisa sobreviver ate o ninho estar carregado; descarta-la"
                        + " no hook de restart faria a colonia crescer so no log.");
        assertTrue(colonia.comida() > 0);
        assertEquals(24_000L, colonia.ultimoTick());
    }

    // -------------------------------------------------------- membros

    @Test
    @DisplayName("membro em chunk descarregado NAO e esquecido")
    void chunkDormindoNaoEsqueceMembro() {
        ChimeraColony colonia = colonia();
        colonia.alistar(MEMBRO);
        colonia.alistar(OUTRO);
        // O mundo diz que MEMBRO ainda existe (dormindo no disco) e OUTRO nao.
        assertEquals(1, colonia.esquecerAusentes(MEMBRO::equals));
        assertEquals(Set.of(MEMBRO), colonia.membros(),
                "Apagar quem esta apenas dormindo faria o membro voltar como orfao,"
                        + " alimentando uma colonia fantasma.");
    }

    @Test
    @DisplayName("o estagio nao regride com a populacao")
    void estagioEMonotono() {
        ChimeraColony colonia = colonia();
        colonia.estagio(ChimeraColonyStage.HUNTING);
        colonia.estagio(ChimeraColonyStage.NEST);
        assertEquals(ChimeraColonyStage.HUNTING, colonia.estagio(),
                "Oscilar com a populacao faria a colonia trocar de comportamento sem causa"
                        + " visivel para o jogador.");
    }

    // ------------------------------------------------------ relatorios

    @Test
    @DisplayName("o relatorio de batedor EXPIRA, e o alerta baixa junto")
    void relatorioExpira() {
        ChimeraColony colonia = colonia();
        colonia.relatar(AMEACA, 0L, 200);
        assertEquals(1, colonia.alerta());
        assertEquals(Set.of(AMEACA), colonia.ameacasConhecidas());

        assertEquals(0, colonia.expirarRelatorios(100L));
        assertEquals(1, colonia.expirarRelatorios(200L));
        assertEquals(0, colonia.alerta(),
                "Sem expiracao a colonia lembraria para sempre de quem passou uma vez, e o"
                        + " alerta nunca baixaria -- uma colonia furiosa sem motivo visivel.");
    }

    @Test
    @DisplayName("o alerta tem teto")
    void alertaTemTeto() {
        ChimeraColony colonia = colonia();
        for (int i = 0; i < 20; i++) {
            colonia.relatar(UUID.randomUUID(), 0L, 100);
        }
        assertEquals(ChimeraColony.ALERTA_MAXIMO, colonia.alerta());
    }

    // ----------------------------------------------------- persistencia

    @Test
    @DisplayName("a colonia atravessa o save, e os relatorios NAO")
    void coloniaAtravessaOSaveSemOsRelatorios() {
        ChimeraColony colonia = colonia();
        colonia.rainha(MEMBRO);
        colonia.alistar(MEMBRO);
        colonia.alistar(OUTRO);
        colonia.alimentar("minecraft:cow", 6);
        colonia.estagio(ChimeraColonyStage.HUNTING);
        colonia.relatar(AMEACA, 0L, 500);
        colonia.autorizarNascimentos(2);
        colonia.marcarTick(1234L);

        ChimeraColony lida = ChimeraColony.carregar(colonia.salvar());
        assertEquals(colonia.id(), lida.id());
        assertEquals(NINHO, lida.ninho());
        assertEquals(MEMBRO, lida.rainha().orElseThrow());
        assertEquals(colonia.membros(), lida.membros());
        assertEquals(6, lida.comida());
        assertEquals(ChimeraColonyStage.HUNTING, lida.estagio());
        assertEquals(1234L, lida.ultimoTick());
        assertEquals(2, lida.nascimentosPendentes(),
                "Nascimentos autorizados nao podem sumir no restart enquanto o ninho esta"
                        + " descarregado.");
        assertTrue(lida.ameacasConhecidas().isEmpty(),
                "Prazo em ticks de mundo salvo atravessaria o restart com o relogio zerado do"
                        + " outro lado, e a colonia acordaria em alerta maximo por uma ameaca"
                        + " de outra sessao.");
    }

    @Test
    @DisplayName("o NBT do gene pool sai em ordem estavel")
    void genePoolEstavel() {
        ChimeraColony a = colonia();
        a.alimentar("minecraft:pig", 2);
        a.alimentar("minecraft:cow", 3);
        ChimeraColony b = colonia();
        b.alimentar("minecraft:cow", 3);
        b.alimentar("minecraft:pig", 2);
        assertEquals(a.salvar().toString(), b.salvar().toString());
    }

    @Test
    @DisplayName("colonia sem ninho reprova")
    void coloniaSemNinhoReprova() {
        assertThrows(NullPointerException.class, () -> new ChimeraColony(ID, null));
    }

    @Test
    @DisplayName("gastar mais do que tem e recusado, e nao vira comida negativa")
    void gastoAcimaDoEstoqueERecusado() {
        ChimeraColony colonia = colonia();
        colonia.alimentar("minecraft:cow", 5);
        assertFalse(colonia.gastar(6));
        assertEquals(5, colonia.comida());
        assertTrue(colonia.gastar(5));
        assertEquals(0, colonia.comida());
    }

    @Test
    @DisplayName("nascimento so e consumido depois da materializacao")
    void nascimentoPendenteEConsumidoUmaVez() {
        ChimeraColony colonia = colonia();
        colonia.autorizarNascimentos(2);
        assertTrue(colonia.consumirNascimento());
        assertEquals(1, colonia.nascimentosPendentes());
        assertTrue(colonia.consumirNascimento());
        assertFalse(colonia.consumirNascimento());
        assertEquals(0, colonia.nascimentosPendentes());
    }
}
