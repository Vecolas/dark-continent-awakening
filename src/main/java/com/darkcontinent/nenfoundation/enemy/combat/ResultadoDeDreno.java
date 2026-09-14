package com.darkcontinent.nenfoundation.enemy.combat;

/**
 * O que sai de uma decisao de dreno: o MOTIVO e, quando ha, quanto curar.
 *
 * <p><b>Os dois campos viajam juntos porque separados eles divergem.</b> Um
 * metodo que devolvesse so a cura obrigaria o chamador a inventar o motivo a
 * partir do zero, e um que devolvesse so o motivo obrigaria o chamador a refazer
 * a conta -- e a conta refeita do lado de fora e como a regra passa a existir em
 * dois lugares. Duas contas para a mesma cura nao dao erro: dao um oficial que
 * cura um valor e contabiliza outro no teto, e o teto para de segurar.</p>
 *
 * <p><b>A invariante e cobrada aqui e nao confiada ao chamador:</b> cura maior
 * que zero se e somente se {@link DecisaoDeDreno#DRENA}. Sem ela, uma recusa com
 * cura junto seria aceita em silencio, e a unica coisa que o teto do encontro faz
 * -- impedir que o combate longo nunca acabe -- deixaria de valer.</p>
 *
 * @param motivo por que drenou, ou por que nao
 * @param cura pontos de vida a devolver; sempre zero quando o motivo nao e DRENA
 */
public record ResultadoDeDreno(DecisaoDeDreno motivo, float cura) {

    public ResultadoDeDreno {
        if (motivo == null) throw new IllegalArgumentException("motivo de dreno ausente");
        if (!Float.isFinite(cura) || cura < 0.0F) {
            throw new IllegalArgumentException("cura invalida: " + cura);
        }
        if ((motivo == DecisaoDeDreno.DRENA) != (cura > 0.0F)) {
            throw new IllegalArgumentException("resultado incoerente: motivo=" + motivo
                    + " cura=" + cura + ". Cura sem DRENA e um dreno que ninguem contabiliza no"
                    + " teto; DRENA sem cura e um teto gasto por nada. Nenhum dos dois levanta"
                    + " erro do lado de fora.");
        }
    }

    /** A recusa, escrita uma vez: motivo preservado, cura zerada por construcao. */
    public static ResultadoDeDreno recusa(DecisaoDeDreno motivo) {
        if (motivo == DecisaoDeDreno.DRENA) {
            throw new IllegalArgumentException("DRENA nao e recusa");
        }
        return new ResultadoDeDreno(motivo, 0.0F);
    }
}
