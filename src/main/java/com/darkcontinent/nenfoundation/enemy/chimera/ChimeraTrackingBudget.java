package com.darkcontinent.nenfoundation.enemy.chimera;

import java.util.Objects;

/**
 * O TETO de formigas que um servidor aguenta rastrear -- e o que acontece ao bater nele.
 *
 * <p><b>A falha que isto impede e de CRESCIMENTO, e ela nao da erro.</b> Uma
 * colonia que produz mais do que mata cresce para sempre. Nao ha excecao, nao ha
 * log: ha um mundo que fica mais lento a cada hora de jogo, e o relato de bug e
 * "o servidor piora com o tempo" -- o mais caro de diagnosticar que existe,
 * porque nada aponta para a causa.</p>
 *
 * <p><b>O teto e por RANK, e nao um numero unico.</b> Trinta peoes e um jogo;
 * trinta oficiais e uma parede. Um teto global deixaria a colonia gastar a cota
 * inteira com os bichos caros, e o resultado seria uma colonia sem tropa e com
 * cinco chefes -- dentro das regras, e errado.</p>
 *
 * <p><b>Bater no teto RECUSA o nascimento; nao mata ninguem.</b> Matar o mais
 * antigo para abrir espaco pareceria elegante e faria entidades sumirem na
 * frente do jogador sem motivo visivel. Recusar e visivel do lado certo: a
 * colonia simplesmente para de crescer.</p>
 */
public record ChimeraTrackingBudget(int maximoDePeoes, int maximoDeOficiais, int maximoTotal) {

    public ChimeraTrackingBudget {
        if (maximoDePeoes < 1 || maximoDeOficiais < 0 || maximoTotal < 1) {
            throw new IllegalArgumentException("orcamento de rastreamento invalido");
        }
        // O TOTAL PRECISA FICAR ENTRE O MAIOR TETO E A SOMA DELES, e os dois lados
        // da faixa tem motivo.
        //
        // Acima da soma, o total nunca morde: os tetos por rank param o crescimento
        // antes, e o total vira um numero que a proxima pessoa gira sem efeito
        // nenhum -- o botao morto que este projeto passa o dia evitando.
        //
        // Abaixo do maior teto, e o teto por rank que nunca morde: o total corta
        // primeiro, e a distincao entre tropa e chefia deixa de existir na pratica.
        // Nenhum dos dois casos da erro; os dois dao um numero que mente.
        int soma = maximoDePeoes + maximoDeOficiais;
        int maiorTeto = Math.max(maximoDePeoes, maximoDeOficiais);
        if (maximoTotal > soma) {
            throw new IllegalArgumentException("o total (" + maximoTotal + ") e maior que a soma"
                    + " dos tetos por rank (" + soma + "): ele nunca morde, e vira botao morto");
        }
        if (maximoTotal < maiorTeto) {
            throw new IllegalArgumentException("o total (" + maximoTotal + ") e menor que o maior"
                    + " teto por rank (" + maiorTeto + "): esse teto nunca morde, e a distincao"
                    + " entre tropa e chefia deixa de existir na pratica");
        }
    }

    /** Orcamento padrao: 24 peoes, 6 oficiais, 28 no total. */
    public static ChimeraTrackingBudget padrao() {
        return new ChimeraTrackingBudget(24, 6, 28);
    }

    /**
     * Cabe mais uma formiga deste rank?
     *
     * @param rank o rank que quer nascer
     * @param peoesVivos quantos peoes (e drudges) o servidor esta rastreando
     * @param oficiaisVivos quantos oficiais ou acima
     */
    public boolean cabe(ChimeraRank rank, int peoesVivos, int oficiaisVivos) {
        Objects.requireNonNull(rank, "rank ausente");
        if (peoesVivos < 0 || oficiaisVivos < 0) {
            throw new IllegalArgumentException("contagem de formigas negativa");
        }
        if (peoesVivos + oficiaisVivos >= maximoTotal) return false;
        return oficial(rank) ? oficiaisVivos < maximoDeOficiais : peoesVivos < maximoDePeoes;
    }

    /**
     * A fronteira entre tropa e chefia.
     *
     * <p>Ela mora AQUI, num lugar so. Espalhada por {@code switch} em cada
     * consumidor, ela divergiria no dia em que um rank novo aparecesse -- e o
     * rank novo cairia no lado errado em metade dos lugares, sem erro nenhum.</p>
     */
    public static boolean oficial(ChimeraRank rank) {
        return switch (Objects.requireNonNull(rank, "rank ausente")) {
            case CONSTRUCTION, PEON, DRUDGE -> false;
            case OFFICER, SQUADRON_LEADER, ROYAL_GUARD, KING -> true;
        };
    }
}
