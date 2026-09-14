package com.darkcontinent.nenfoundation.enemy.combat;

/**
 * Regras de stagger: quanto aguenta, quanto absorve, quanto esquece e quanto dura.
 *
 * <p><b>Por que stagger nao e knockback</b> (issue #137). Knockback vanilla e
 * deslocamento: ele empurra e acaba. O que o combate deste mod precisa e uma
 * INTERRUPCAO -- a carga do great stamp para, a bocada do sapo solta, o oficial
 * perde a janela. Usar knockback como substituto funciona ate alguem dar
 * resistencia a knockback ao mob, e entao a interrupcao some junto, sem que nada
 * acuse: o bicho simplesmente nunca mais e interrompido, e o ponto fraco vira
 * decoracao.</p>
 *
 * <p>Os quatro numeros se leem JUNTOS. Resistencia maior que o valor de um golpe
 * comum significa que so golpe pesado acumula; decaimento maior que o ritmo de
 * ataque do jogador significa que acumular e impossivel. Nenhum dos dois da
 * erro -- os dois dao um mob que nunca cambaleia.</p>
 *
 * @param limiar quanto de stagger acumulado dispara a interrupcao
 * @param resistencia quanto cada golpe perde antes de entrar na conta
 * @param decaimentoPorTick quanto do acumulado se perde a cada tick sem apanhar
 * @param ticksDeStagger quanto tempo a interrupcao dura depois de disparar
 */
public record StaggerRules(float limiar, float resistencia, float decaimentoPorTick, int ticksDeStagger) {
    public StaggerRules {
        if (!Float.isFinite(limiar) || limiar <= 0.0F
                || !Float.isFinite(resistencia) || resistencia < 0.0F
                || !Float.isFinite(decaimentoPorTick) || decaimentoPorTick < 0.0F
                || ticksDeStagger < 1) {
            throw new IllegalArgumentException("regras de stagger invalidas");
        }
        if (resistencia >= limiar) {
            throw new IllegalArgumentException("resistencia (" + resistencia + ") maior ou igual ao"
                    + " limiar (" + limiar + "): nenhum golpe acumularia, e o mob nunca cambalearia"
                    + " -- sem erro nenhum, so um ponto fraco que nao funciona");
        }
    }

    /** O que sobra de um golpe depois da resistencia; nunca negativo. */
    public float efetivo(float valorBruto) {
        if (!Float.isFinite(valorBruto) || valorBruto < 0.0F) {
            throw new IllegalArgumentException("valor de stagger invalido: " + valorBruto);
        }
        return Math.max(0.0F, valorBruto - resistencia);
    }
}
