package com.darkcontinent.nenfoundation.enemy.ai;

import java.util.Objects;

/**
 * A decisao de voo de UM tick: o que fazer, e quanto subir enquanto faz.
 *
 * <p><b>Os dois campos viajam juntos porque a altura nao e um movimento
 * separado.</b> Um batedor que fugisse na horizontal e so depois subisse passaria
 * pela altura do jogador no caminho -- ou seja, entregaria de graca o unico
 * instante em que um arco acerta um bicho de 16 de vida. Subir ENQUANTO afasta e o
 * comportamento; dois estados alternados seriam outro bicho.</p>
 *
 * <p><b>{@code ganhoDeAltura} nunca e negativo.</b> Descer por vontade propria nao
 * e uma decisao que este mob toma: a unica descida dele e {@link MovimentoDeVoo#CAIR},
 * e essa e imposta pelo cambaleio. Permitir negativo aqui abriria a porta para um
 * batedor que "escolhe" descer ate o chao e fica indistinguivel de um mob terrestre,
 * sem que nada reprove.</p>
 *
 * @param movimento o que fazer neste tick
 * @param ganhoDeAltura quantos blocos acima da posicao atual o voo deve mirar; 0
 *        quando o batedor ja esta na altura que queria, ou quando nao manda no voo
 */
public record DecisaoDeVoo(MovimentoDeVoo movimento, double ganhoDeAltura) {

    public DecisaoDeVoo {
        Objects.requireNonNull(movimento, "decisao de voo sem movimento: nulo aqui viraria"
                + " NullPointerException no meio de um tick de servidor, com pilha que nao diz"
                + " qual regra deixou de responder");
        if (!Double.isFinite(ganhoDeAltura) || ganhoDeAltura < 0.0D) {
            throw new IllegalArgumentException("ganho de altura invalido: " + ganhoDeAltura
                    + ". Negativo seria um batedor que decide descer, e descer e o que o cambaleio"
                    + " impoe -- nunca o que ele escolhe. NaN viraria uma posicao alvo NaN e o mob"
                    + " pararia de se mover sem uma linha de log.");
        }
    }

    /** Decisao sem ganho de altura: o movimento ja diz tudo. */
    public static DecisaoDeVoo de(MovimentoDeVoo movimento) {
        return new DecisaoDeVoo(movimento, 0.0D);
    }

    /** O batedor manda no proprio voo neste tick? Falso apenas em {@link MovimentoDeVoo#CAIR}. */
    public boolean controlaOVoo() { return movimento != MovimentoDeVoo.CAIR; }
}
