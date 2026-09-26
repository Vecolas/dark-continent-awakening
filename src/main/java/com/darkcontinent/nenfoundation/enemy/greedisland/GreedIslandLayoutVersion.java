package com.darkcontinent.nenfoundation.enemy.greedisland;

/**
 * A versao do layout macro de Greed Island. Secao 88 do documento.
 *
 * <p><b>ELA NAO E A VERSAO DO MOD, e a diferenca e o ponto inteiro.</b> O mod
 * sobe de versao o tempo todo; o layout so pode subir quando a GEOGRAFIA muda,
 * e mudar a geografia de um mundo que ja existe reescreve o mapa que o jogador
 * aprendeu. Um mundo guarda a versao com que nasceu.
 *
 * <p>O que conta como mudanca incompativel:
 *
 * <ul>
 *   <li>mover uma ancora de cidade;
 *   <li>mover uma ancora de costa, peninsula ou baia;
 *   <li>mudar a semente do layout;
 *   <li>mudar a formula da mascara, do relevo ou da hidrografia;
 *   <li>renomear ou reposicionar uma macro-regiao.
 * </ul>
 *
 * <p>O que NAO conta: vegetacao, rocha solta, caverna pequena, ajuste de
 * spawn -- a microgeografia e procedural por seed do mundo e pode mudar.
 *
 * <p><b>REGRA DE MIGRACAO, da secao 128:</b> chunk de protótipo NAO vira chunk
 * de layout v1. Durante o desenvolvimento a dimensao e apagada e regenerada. A
 * v1 precisa CONGELAR antes do release -- depois disso, uma versao nova exige
 * plano para os mundos existentes, do mesmo jeito que o {@code ADR-004} exige
 * para o formato de save.
 */
public final class GreedIslandLayoutVersion {

    /**
     * A versao atual.
     *
     * <p>ZERO E O PROTOTIPO, e isso e deliberado: enquanto este numero for 0, a
     * ilha e andaime e nenhum mundo deve ser considerado permanente. A v1 e a
     * macrogeografia projetada, e ela ainda nao existe.
     */
    public static final int ATUAL = 0;

    /** A primeira versao jogavel, quando existir. */
    public static final int PRIMEIRA_CONGELADA = 1;

    /** Se a geografia de hoje pode sustentar um mundo de verdade. */
    public static boolean ehPrototipo() {
        return ATUAL < PRIMEIRA_CONGELADA;
    }

    /**
     * A nota que a secao 129 exige, em voz alta e num lugar que o codigo le.
     *
     * <p>Ela existe para impedir que o disco seja aceito como Definition of
     * Done -- o documento nomeia esse risco por escrito.
     */
    public static final String AVISO_DE_PROTOTIPO =
            "Current circular mask is a development scaffold and not a production "
                    + "representation of Greed Island.";

    private GreedIslandLayoutVersion() {
    }
}
