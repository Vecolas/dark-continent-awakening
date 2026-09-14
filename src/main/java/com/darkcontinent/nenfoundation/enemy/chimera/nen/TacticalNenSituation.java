package com.darkcontinent.nenfoundation.enemy.chimera.nen;

/**
 * Os fatos que a formiga JA mediu -- o controlador nao consulta nada.
 *
 * <p>Todo campo aqui e apurado pelo lado autoritativo. E isso que permite testar
 * a decisao tatica sem servidor e garante que nenhum dado de cliente entre nela.
 * E e tambem o que mantem a fronteira do CLAUDE.md: a aura chega como uma FRACAO
 * ja calculada pelo nucleo, e nao como um pool que este pacote poderia aprender
 * a manipular.</p>
 *
 * @param fracaoDeAura aura atual sobre o maximo, 0..1, medida pelo nucleo de Nen
 * @param fracaoDeVida vida atual sobre a maxima, 0..1
 * @param emCombate ha um alvo hostil reconhecido agora
 * @param alvoAoAlcance o alvo esta perto o bastante para o golpe valer
 * @param alvoEscondido ha sinal de algo que a visao comum nao explica
 * @param sobPressao mais de um atacante, ou dano pesado recente
 * @param podeFugir ha para onde recuar; sem saida, sumir so entrega a morte
 */
public record TacticalNenSituation(double fracaoDeAura, double fracaoDeVida, boolean emCombate,
        boolean alvoAoAlcance, boolean alvoEscondido, boolean sobPressao, boolean podeFugir) {

    public TacticalNenSituation {
        if (!faixa(fracaoDeAura) || !faixa(fracaoDeVida)) {
            throw new IllegalArgumentException("fracao fora de 0..1: aura=" + fracaoDeAura
                    + " vida=" + fracaoDeVida + ". Um valor fora da faixa nao daria erro na"
                    + " decisao -- daria uma formiga que se acha cheia de aura para sempre.");
        }
        if (alvoAoAlcance && !emCombate) {
            throw new IllegalArgumentException("alvo ao alcance sem combate: os dois campos"
                    + " discordam, e a decisao tomaria o caminho de ataque contra um alvo que o"
                    + " resto do sistema diz nao existir");
        }
    }

    private static boolean faixa(double valor) {
        return Double.isFinite(valor) && valor >= 0.0D && valor <= 1.0D;
    }

    /** Situacao de quem esta em paz: cheio, inteiro e sozinho. */
    public static TacticalNenSituation tranquila() {
        return new TacticalNenSituation(1.0D, 1.0D, false, false, false, false, true);
    }
}
