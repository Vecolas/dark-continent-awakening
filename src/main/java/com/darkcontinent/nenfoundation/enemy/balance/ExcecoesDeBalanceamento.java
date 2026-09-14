package com.darkcontinent.nenfoundation.enemy.balance;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Os bichos que FICAM fora da faixa do proprio papel -- de propósito, e por escrito.
 *
 * <p><b>Por que uma lista de excecoes, e nao uma faixa mais frouxa.</b> A faixa
 * mede DURABILIDADE; o {@code ThreatTier} mede AMEACA DE ENCONTRO, e o javadoc
 * dele diz isso com todas as letras. Os dois eixos quase sempre andam juntos, e
 * e por isso que a faixa e util -- mas nem sempre: um emboscador fragil cuja
 * forca e o engano e perigoso E morre rapido, e as duas coisas sao verdade ao
 * mesmo tempo.</p>
 *
 * <p>Afrouxar a faixa para caber nele destruiria a regua para todos os outros. A
 * alternativa honesta e a que este repositorio ja usa para corpo emprestado
 * (ADR-017): a excecao existe, ela se DECLARA, e a lista <b>morde dos dois
 * lados</b> -- bicho fora da faixa e fora da lista reprova, e bicho na lista que
 * voltou para dentro da faixa TAMBEM reprova. Sem a segunda metade, a lista
 * passaria a cobrir em silencio o dia em que a excecao deixasse de existir, e um
 * ELITE que virou saco de pancada por engano ficaria coberto por ela.</p>
 */
public final class ExcecoesDeBalanceamento {

    private static final Map<String, String> MOTIVOS = new LinkedHashMap<>();

    static {
        MOTIVOS.put("man_faced_ape",
                "Emboscador FRAGIL de propósito. O perfil dele diz, com todas as letras, que"
                        + " dar a ele um corpo que aguenta troca de golpes premiaria justamente"
                        + " o jogador que NAO percebeu o disfarce -- a pista observavel viraria"
                        + " enfeite. A ameaca dele e o bando revelando junto, e nao o couro;"
                        + " ele morre em pouco mais de dois segundos e continua sendo DANGEROUS"
                        + " porque o encontro e perigoso, e nao porque o bicho e duro.");
    }

    private ExcecoesDeBalanceamento() { }

    /** O motivo declarado, se este id for excecao. */
    public static Optional<String> motivo(String id) {
        return Optional.ofNullable(MOTIVOS.get(id));
    }

    public static boolean declarado(String id) { return MOTIVOS.containsKey(id); }

    /** A lista inteira, para o portao poder cobrar o outro lado. */
    public static Map<String, String> todos() { return Map.copyOf(MOTIVOS); }
}
