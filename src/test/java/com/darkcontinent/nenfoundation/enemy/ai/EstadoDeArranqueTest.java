package com.darkcontinent.nenfoundation.enemy.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao do RELOGIO do arranque -- a fila de fases que cobra o preco.
 *
 * <p>{@code RegrasDeArranqueTest} prova a decisao; este arquivo prova o
 * ANDAMENTO. Sao coisas diferentes, e a segunda e onde moram os defeitos mais
 * silenciosos: uma fase que nao avanca, um pedido recusado que zera o relogio, um
 * multiplicador congelado. Nenhum deles levanta excecao -- todos aparecem como
 * "esse bicho esta estranho".</p>
 */
class EstadoDeArranqueTest {

    private static final int ARRANQUE = 30;
    private static final int FADIGA = 45;
    private static final int RECARGA = 60;
    private static final double RAPIDO = 1.55D;
    private static final double LENTO = 0.55D;
    private static final double MINIMA = 6.0D;
    private static final double MAXIMA = 26.0D;
    private static final double ABERTURA = 4.0D;
    private static final double SEM_ALIADO = Double.POSITIVE_INFINITY;

    private static EstadoDeArranque estado() {
        return new EstadoDeArranque(new RegrasDeArranque(ARRANQUE, FADIGA, RECARGA, RAPIDO, LENTO,
                MINIMA, MAXIMA, ABERTURA));
    }

    private static void avancar(EstadoDeArranque estado, int ticks) {
        for (int i = 0; i < ticks; i++) estado.tick();
    }

    // ------------------------------------------------------------ caso normal

    @Test
    @DisplayName("o ciclo inteiro anda na ordem, e cada fase dura exatamente o declarado")
    void oCicloAndaNaOrdemEComOPrazoCerto() {
        EstadoDeArranque estado = estado();
        assertEquals(FaseDoArranque.PRONTA, estado.fase());
        assertEquals(1.0D, estado.multiplicadorDeVelocidade());

        assertEquals(DecisaoDeArranque.ARRANCAR,
                estado.tentar(12.0D, SEM_ALIADO, true, true));
        assertEquals(FaseDoArranque.ARRANCANDO, estado.fase());
        assertEquals(RAPIDO, estado.multiplicadorDeVelocidade());

        avancar(estado, ARRANQUE - 1);
        assertEquals(FaseDoArranque.ARRANCANDO, estado.fase(),
                "um tick antes do fim ele AINDA esta arrancando -- se virasse aqui, o arranque"
                        + " duraria um tick a menos do que o escrito e ninguem perceberia");
        estado.tick();
        assertEquals(FaseDoArranque.FATIGADA, estado.fase());
        assertEquals(LENTO, estado.multiplicadorDeVelocidade(),
                "e aqui que o preco aparece na tela: sem a troca de multiplicador, a fadiga seria"
                        + " um estado que so o servidor conhece");

        avancar(estado, FADIGA - 1);
        assertEquals(FaseDoArranque.FATIGADA, estado.fase());
        estado.tick();
        assertEquals(FaseDoArranque.EM_RECARGA, estado.fase());
        assertEquals(1.0D, estado.multiplicadorDeVelocidade());

        avancar(estado, RECARGA - 1);
        assertEquals(FaseDoArranque.EM_RECARGA, estado.fase());
        estado.tick();
        assertEquals(FaseDoArranque.PRONTA, estado.fase(),
                "fechar o ciclo em PRONTA e o que permite o proximo arranque. Parar em EM_RECARGA"
                        + " daria um bicho que arranca uma vez na vida e passa o resto do encontro"
                        + " em velocidade normal -- sem erro, so sem ficha");
        assertEquals(0, estado.ticksNaFase());
    }

    @Test
    @DisplayName("PRONTA nao conta ticks: ela espera uma decisao, nao um prazo")
    void prontaNaoContaTicks() {
        EstadoDeArranque estado = estado();
        avancar(estado, 10_000);
        assertEquals(FaseDoArranque.PRONTA, estado.fase());
        assertEquals(0, estado.ticksNaFase(),
                "um contador que cresce em PRONTA subiria sem limite ate estourar o int, e um"
                        + " contador negativo compararia mal contra a duracao -- devolvendo o bicho"
                        + " ao arranque no tick seguinte, do nada");
    }

    @Test
    @DisplayName("depois do ciclo inteiro ela pode arrancar de novo")
    void oSegundoArranqueSaiDepoisDoCicloInteiro() {
        EstadoDeArranque estado = estado();
        estado.tentar(12.0D, SEM_ALIADO, true, true);
        avancar(estado, ARRANQUE + FADIGA + RECARGA);
        assertEquals(FaseDoArranque.PRONTA, estado.fase());
        assertEquals(DecisaoDeArranque.ARRANCAR, estado.tentar(12.0D, SEM_ALIADO, true, true));
    }

    // --------------------------------------------------------------- recusado

    @Test
    @DisplayName("pedido RECUSADO nao mexe no relogio -- e esta e a armadilha principal")
    void aRecusaNaoReiniciaARelogio() {
        EstadoDeArranque estado = estado();
        estado.tentar(12.0D, SEM_ALIADO, true, true);
        avancar(estado, ARRANQUE + 5);
        assertEquals(FaseDoArranque.FATIGADA, estado.fase());
        assertEquals(5, estado.ticksNaFase());

        // Uma IA que pede o arranque em todo passo de coordenacao: o pedido e
        // recusado, e o relogio TEM de continuar andando.
        for (int i = 0; i < 10; i++) {
            assertEquals(DecisaoDeArranque.FATIGADA, estado.tentar(12.0D, SEM_ALIADO, true, true));
            estado.tick();
        }
        assertEquals(15, estado.ticksNaFase(),
                "se a recusa zerasse o contador, um bicho que tenta arrancar todo tick ficaria"
                        + " LENTO PARA SEMPRE: a IA continuaria pedindo, o servidor continuaria"
                        + " recusando, e o log ficaria limpo");
        assertEquals(FaseDoArranque.FATIGADA, estado.fase());
    }

    @Test
    @DisplayName("recusa por distancia ou por aliado tambem nao muda a fase")
    void recusaPorMedidaNaoMudaAFase() {
        EstadoDeArranque estado = estado();
        assertEquals(DecisaoDeArranque.PERTO_DEMAIS,
                estado.tentar(MINIMA - 1.0D, SEM_ALIADO, true, true));
        assertEquals(FaseDoArranque.PRONTA, estado.fase());
        assertEquals(DecisaoDeArranque.ALIADO_JA_ABRIU,
                estado.tentar(12.0D, ABERTURA - 1.0D, true, true));
        assertEquals(FaseDoArranque.PRONTA, estado.fase(),
                "uma recusa que gastasse a fase entregaria o preco sem o ganho -- o bicho pagaria"
                        + " a fadiga por um arranque que nunca aconteceu");
        assertEquals(1.0D, estado.multiplicadorDeVelocidade());
    }

    @Test
    @DisplayName("limpar devolve tudo ao repouso; regras ausentes sao recusadas")
    void aLimpezaEOContratoDeSaida() {
        EstadoDeArranque estado = estado();
        estado.tentar(12.0D, SEM_ALIADO, true, true);
        avancar(estado, ARRANQUE + 3);
        assertEquals(FaseDoArranque.FATIGADA, estado.fase());

        estado.limpar();
        assertEquals(FaseDoArranque.PRONTA, estado.fase(),
                "sem limpeza, uma entidade reaproveitada volta a vida no meio de uma fadiga que ela"
                        + " nunca pagou, e o jogador ve um guepardo nascer lento sem causa nenhuma");
        assertEquals(0, estado.ticksNaFase());
        assertEquals(1.0D, estado.multiplicadorDeVelocidade());

        assertThrows(NullPointerException.class, () -> new EstadoDeArranque(null),
                "sem regras nao ha duracao, nao ha fadiga e nao ha teto -- e um arranque sem teto e"
                        + " velocidade permanente");
    }

    // ------------------------------------------- o caso que DEVE reprovar

    @Test
    @DisplayName("PORTAO: o multiplicador e DERIVADO da fase, e nunca o mesmo o ciclo inteiro")
    void oMultiplicadorNaoPodeCongelar() {
        EstadoDeArranque estado = estado();
        estado.tentar(12.0D, SEM_ALIADO, true, true);
        double noArranque = estado.multiplicadorDeVelocidade();

        avancar(estado, ARRANQUE);
        double naFadiga = estado.multiplicadorDeVelocidade();
        avancar(estado, FADIGA);
        double naRecarga = estado.multiplicadorDeVelocidade();

        // As tres leituras saem do MESMO objeto, sem nada ter sido reatribuido. Se
        // alguem trocar a derivacao por um campo escrito no instante do arranque,
        // as tres passam a ser iguais -- e o bicho corre acelerado durante a
        // propria fadiga, com a fase certa no debug e o clipe certo na tela.
        assertNotEquals(noArranque, naFadiga,
                "arranque e fadiga leram o MESMO multiplicador: o valor foi congelado em vez de"
                        + " derivado da fase, e a fadiga deixou de custar alguma coisa");
        assertNotEquals(noArranque, naRecarga);
        assertEquals(RAPIDO, noArranque);
        assertEquals(LENTO, naFadiga);
        assertEquals(1.0D, naRecarga);
    }
}
