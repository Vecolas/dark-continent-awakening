package com.darkcontinent.nenfoundation.enemy.combat;

import java.util.Objects;

/**
 * UM golpe de uma sequencia: o relogio dele e a caixa que ele reivindica.
 *
 * <p><b>Os dois andam juntos porque eles divergem calados.</b> A alternativa
 * obvia -- uma lista de {@link AttackDefinition} aqui e uma lista de
 * {@link AttackHitbox} ali, casadas por indice -- funciona ate alguem
 * acrescentar, remover ou reordenar um golpe em so uma das duas. O resultado nao
 * e erro de compilacao nem excecao: e o segundo golpe da sequencia acertando com
 * o alcance do terceiro. O dano e o certo, a fase e a certa, o log fica limpo, e
 * o jogador apanha de um membro que na tela parou antes dele.</p>
 *
 * <p><b>O dano NAO mora aqui</b> -- ele mora dentro da {@link AttackDefinition},
 * que e quem o {@link AttackController} copia no inicio da instancia. Repetir o
 * numero neste record criaria a terceira fonte para a mesma verdade.</p>
 *
 * @param definicao janelas, dano, empurrao e o que pode interromper cada fase
 * @param caixa alcance LOCAL deste golpe; a transformacao para o mundo e do servidor
 */
public record GolpeEncadeado(AttackDefinition definicao, AttackHitbox caixa) {
    public GolpeEncadeado {
        Objects.requireNonNull(definicao, "golpe encadeado sem definicao: ele nao teria janela"
                + " nenhuma, e o relogio do ataque nunca sairia de WINDUP");
        Objects.requireNonNull(caixa, "golpe encadeado sem caixa: a janela ACTIVE abriria e nao"
                + " haveria volume para testar, entao o golpe machucaria ninguem -- sem erro, e"
                + " com o telegrafo inteiro prometendo dano");
    }

    /** Quanto este golpe alcanca a frente, em blocos. O gerador de arte cobra este numero. */
    public double alcanceEmBlocos() { return caixa.maxZ(); }
}
