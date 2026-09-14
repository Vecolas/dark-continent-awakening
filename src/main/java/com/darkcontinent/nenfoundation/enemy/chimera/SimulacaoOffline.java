package com.darkcontinent.nenfoundation.enemy.chimera;

import java.util.Objects;

/**
 * O que uma colonia ganha pelo tempo em que ninguem estava olhando -- COM TETO.
 *
 * <p><b>A falha que este arquivo inteiro existe para impedir e a mais tentadora
 * de todas:</b> "o jogador ficou tres dias fora, entao a colonia cresceu tres
 * dias". Implementada literalmente, essa frase materializa centenas de entidades
 * no instante em que o chunk carrega -- o servidor engasga, o jogador volta para
 * um exercito, e nada disso aparece como erro: cada formiga e legitima, e o
 * calculo esta "certo". A issue #144 e explicita: chunk descarregado usa tempo
 * decorrido LIMITADO.</p>
 *
 * <p><b>O teto e de TEMPO, e nao de resultado.</b> Limitar o resultado
 * ("cresceu no maximo 5") faria um dia e um mes renderem igual, e o tempo perderia
 * o sentido. Limitando o tempo, uma ausencia curta rende proporcionalmente e uma
 * ausencia longa rende o teto -- que e a leitura que o jogador consegue prever.</p>
 *
 * <p><b>Ela nao cria nada.</b> Devolve NUMEROS. Quem materializa entidade e o
 * lado que tem mundo, e so quando o orcamento de rastreamento permitir. Misturar
 * as duas coisas aqui e como a recuperacao vira spawn em massa sem que ninguem
 * possa ver a conta antes.</p>
 *
 * @param ticksMaximosRecuperados o teto; acima disso o tempo extra nao rende nada
 * @param ticksPorCiclo quanto tempo vale um ciclo de crescimento
 * @param comidaPorCiclo quanta comida a colonia junta por ciclo
 * @param nascimentosPorCiclo quantas formigas o ciclo AUTORIZA -- nao cria
 */
public record SimulacaoOffline(int ticksMaximosRecuperados, int ticksPorCiclo,
        int comidaPorCiclo, int nascimentosPorCiclo) {

    public SimulacaoOffline {
        if (ticksMaximosRecuperados < 1 || ticksPorCiclo < 1
                || comidaPorCiclo < 0 || nascimentosPorCiclo < 0) {
            throw new IllegalArgumentException("simulacao offline invalida");
        }
        if (ticksMaximosRecuperados < ticksPorCiclo) {
            throw new IllegalArgumentException("o teto (" + ticksMaximosRecuperados + " ticks) e"
                    + " menor que um ciclo (" + ticksPorCiclo + "): nenhuma ausencia jamais"
                    + " renderia nada, e a colonia congelaria enquanto ninguem olha -- sem erro"
                    + " nenhum, e com o jogador achando que o sistema nao existe");
        }
    }

    /**
     * Padrao: recupera no maximo UM dia de jogo, em ciclos de cinco minutos.
     *
     * <p>Um dia (24000 ticks) e o teto porque e o horizonte que o jogador
     * consegue narrar: "sai ontem e voltei hoje". Acima disso a diferenca entre
     * tres dias e tres semanas deixaria de ser previsivel.</p>
     */
    public static SimulacaoOffline padrao() {
        return new SimulacaoOffline(24_000, 6_000, 4, 1);
    }

    /**
     * Quantos ciclos completos cabem na ausencia, ja com o teto aplicado.
     *
     * <p>Ciclos COMPLETOS, e nao fracao: meia colonia crescida seria um numero
     * quebrado que o resto do sistema teria de arredondar em algum lugar -- e
     * arredondar em dois lugares diferentes e como dois calculos divergem.</p>
     */
    public int ciclos(long ticksDecorridos) {
        if (ticksDecorridos < 0) {
            throw new IllegalArgumentException("ausencia negativa: " + ticksDecorridos
                    + ". Um relogio que anda para tras vem de save adulterado ou de troca de"
                    + " fuso, e tratar isso como crescimento daria uma colonia que encolhe.");
        }
        long limitado = Math.min(ticksDecorridos, ticksMaximosRecuperados);
        return (int) (limitado / ticksPorCiclo);
    }

    /**
     * O que a ausencia rendeu -- em numeros, e ja cortado pelo orcamento.
     *
     * @param ticksDecorridos quanto tempo passou desde o ultimo tick da colonia
     * @param orcamento o teto de rastreamento do servidor
     * @param peoesVivos quantos peoes o servidor ja rastreia
     * @param oficiaisVivos quantos oficiais ja rastreia
     */
    public ResultadoOffline recuperar(long ticksDecorridos, ChimeraTrackingBudget orcamento,
            int peoesVivos, int oficiaisVivos) {
        Objects.requireNonNull(orcamento, "orcamento ausente");
        int ciclos = ciclos(ticksDecorridos);
        if (ciclos == 0) return ResultadoOffline.NADA;

        int comida = ciclos * comidaPorCiclo;
        int autorizados = 0;
        int peoes = peoesVivos;
        // O orcamento e consultado A CADA nascimento, e nao uma vez no fim. Uma
        // checagem unica com o total autorizaria uma leva inteira contra a
        // contagem ANTIGA, e o teto seria estourado de uma vez -- exatamente o
        // pico que ele existe para impedir.
        for (int i = 0; i < ciclos * nascimentosPorCiclo; i++) {
            if (!orcamento.cabe(ChimeraRank.PEON, peoes, oficiaisVivos)) break;
            autorizados++;
            peoes++;
        }
        return new ResultadoOffline(ciclos, comida, autorizados);
    }

    /**
     * O rendimento de uma ausencia. NUMEROS, nunca entidades.
     *
     * @param ciclos quantos ciclos completos couberam no teto
     * @param comida quanto o food score sobe
     * @param nascimentosAutorizados quantas formigas o mundo PODE materializar
     */
    public record ResultadoOffline(int ciclos, int comida, int nascimentosAutorizados) {
        public static final ResultadoOffline NADA = new ResultadoOffline(0, 0, 0);

        public ResultadoOffline {
            if (ciclos < 0 || comida < 0 || nascimentosAutorizados < 0) {
                throw new IllegalArgumentException("rendimento offline negativo");
            }
        }

        public boolean rendeuAlgo() { return ciclos > 0; }
    }
}
