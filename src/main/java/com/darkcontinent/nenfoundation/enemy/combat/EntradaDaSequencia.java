package com.darkcontinent.nenfoundation.enemy.combat;

import java.util.Objects;

/**
 * Os fatos que o servidor JA mediu -- a sequencia nao consulta nada.
 *
 * <p>Todo campo aqui e apurado pelo lado autoritativo, e nenhum vem de pacote de
 * cliente. E isso que permite decidir um combo inteiro sem servidor, num teste
 * unitario, e e tambem o que impede este pacote de aprender a olhar para o mundo:
 * uma regra que sabe consultar entidade e uma regra que so pode ser provada
 * ligando o jogo.</p>
 *
 * <p><b>A fase e os ticks restantes vem do {@link AttackController}, e nao de um
 * contador proprio.</b> Um contador aqui seria o segundo relogio de ataque -- os
 * dois andariam juntos ate a primeira interrupcao e depois discordariam por um
 * tick, que e invisivel em teste e visivel na tela.</p>
 *
 * @param fase fase corrente do golpe em curso, lida do controlador de ataque
 * @param ticksRestantesDaFase quanto falta para a fase corrente terminar
 * @param cambaleando o mob esta interrompido AGORA
 * @param alvoValido ha um alvo vivo em quem continuar o combo
 * @param podeComecar o controlador de ataque aceita iniciar um golpe agora
 * @param recuando o mob decidiu recuar (vida critica, ou intencao tatica de sumir)
 */
public record EntradaDaSequencia(AttackPhase fase, int ticksRestantesDaFase, boolean cambaleando,
        boolean alvoValido, boolean podeComecar, boolean recuando) {

    public EntradaDaSequencia {
        Objects.requireNonNull(fase, "entrada de sequencia sem fase: sem ela a decisao nao tem"
                + " como saber se ha golpe em curso, e o combo recomecaria por cima de si mesmo");
        if (ticksRestantesDaFase < 0) {
            throw new IllegalArgumentException("ticks restantes negativos (" + ticksRestantesDaFase
                    + "): um contador que passou do zero faria a conta da emenda dar um numero"
                    + " maior do que a recuperacao inteira, e o encadeado sairia antes da hora"
                    + " -- sem erro, e com dois golpes sobrepostos na tela");
        }
    }

    /** Situacao de quem esta em combate, com o relogio parado e pronto para comecar. */
    public static EntradaDaSequencia pronta() {
        return new EntradaDaSequencia(AttackPhase.IDLE, 0, false, true, true, false);
    }
}
