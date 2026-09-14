package com.darkcontinent.nenfoundation.enemy.combat;

import java.util.Objects;

/**
 * A janela em que o besouro esta de costas, de UMA entidade.
 *
 * <p><b>Estado de instancia, nunca de classe.</b> Este projeto ja sabe que vai
 * cometer o erro de guardar estado de jogador num campo da classe da tecnica; a
 * versao dele aqui seria um contador estatico, e o sintoma seria um besouro
 * virando quando o outro apanha. Uma instancia por mob, criada no construtor da
 * entidade e apagada no {@code limpar()} dela.</p>
 *
 * <p><b>Por que um contador e nao um instante de mundo.</b> Guardar "o tick em
 * que ele caiu" e comparar com {@code tickCount} funciona ate a entidade trocar
 * de dimensao ou o mundo ser recarregado, quando o relogio nao e mais o mesmo. O
 * sintoma seria um besouro que nasce de costas por horas, ou que se levanta no
 * mesmo tick em que cai. Contador nao depende de relogio nenhum.</p>
 *
 * <p>Ele nao sobrevive a save: um besouro salvo de costas volta de pe. Isso e a
 * separacao do ADR-002 -- janela de combate e runtime, e runtime nao sobrevive a
 * nada. Vale dizer em voz alta porque o contrario tambem seria plausivel, e
 * escolher por acidente e o que produz save corrompido meses depois.</p>
 */
public final class EstadoDeViragem {

    private final RegrasDeViragem regras;
    private int ticksRestantes;

    public EstadoDeViragem(RegrasDeViragem regras) {
        this.regras = Objects.requireNonNull(regras, "regras de viragem ausentes");
    }

    public RegrasDeViragem regras() { return regras; }

    public int ticksRestantes() { return ticksRestantes; }

    /** De costas: o ventre paga e o bicho nao ataca. */
    public boolean deCostas() { return ticksRestantes > 0; }

    /**
     * Os ultimos ticks da janela, em que ele ja esta se endireitando.
     *
     * <p>Ele existe para o CLIENTE poder mostrar que a janela vai acabar. Sem
     * esse aviso, a pose de costas seria identica do primeiro ao ultimo tick e o
     * jogador levaria a investida seguinte no meio de um golpe -- encurtar a
     * janela sem dizer transforma o quebra-cabeca em sorte, e este e o unico
     * ponto do bicho em que "sem dizer" seria de graca.</p>
     */
    public boolean levantando() {
        return ticksRestantes > 0 && ticksRestantes <= regras.ticksParaLevantar();
    }

    /**
     * Arma a janela. SEGUNDA tranca contra a viragem repetida.
     *
     * <p>A primeira e {@link DecisaoDeViragem#JA_ESTA_DE_COSTAS}, decidida pela
     * regra. Esta aqui existe porque as duas perguntas vivem em lugares
     * diferentes do tick, e no dia em que alguem chamar {@code virar()} de um
     * caminho novo sem passar pela regra, a janela seria renovada a cada golpe e
     * o besouro nunca mais se levantaria. Nao da erro: da um chefe que morre de
     * costas e um encontro que desaparece.</p>
     *
     * @return true se esta chamada de fato derrubou o bicho
     */
    public boolean virar() {
        if (deCostas()) return false;
        ticksRestantes = regras.ticksDeCostas();
        return true;
    }

    /**
     * Um tick da janela.
     *
     * @return true no tick EXATO em que ele volta a ficar de pe, para quem
     *         precisa devolver o mob ao estado anterior sem vigiar o contador
     */
    public boolean tick() {
        if (ticksRestantes == 0) return false;
        ticksRestantes--;
        return ticksRestantes == 0;
    }

    /**
     * Limpeza: morte, remocao, unload, troca de dimensao.
     *
     * <p>Quem liga, desliga. Um besouro removido no meio da janela e recriado
     * pelo carregamento do chunk voltaria com o contador de outra entidade se
     * este estado fosse compartilhado -- e sem esta limpeza, o campo de fase
     * sincronizado ficaria publicando "de costas" para um cadaver.</p>
     */
    public void limpar() {
        ticksRestantes = 0;
    }
}
