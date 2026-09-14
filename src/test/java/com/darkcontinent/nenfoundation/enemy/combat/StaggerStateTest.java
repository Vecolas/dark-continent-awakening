package com.darkcontinent.nenfoundation.enemy.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao do stagger como sistema PROPRIO (issue #137).
 *
 * <p>A falha que ele evita e a substituicao por knockback: funciona ate alguem
 * dar resistencia a knockback ao mob, e entao a interrupcao some sem que nada
 * acuse. Aqui o stagger tem limiar, resistencia e decaimento proprios, e o teste
 * cobra os tres.</p>
 */
class StaggerStateTest {

    private static StaggerRules regras() {
        return new StaggerRules(10.0F, 2.0F, 0.5F, 30);
    }

    @Test
    @DisplayName("golpe abaixo da resistencia e ABSORVIDO e nao acumula nada")
    void resistenciaComeGolpeFraco() {
        StaggerState estado = new StaggerState(regras());
        assertEquals(StaggerResult.ABSORVIDO, estado.acumular(1L, 2.0F));
        assertEquals(0.0F, estado.acumulado());
        assertFalse(estado.cambaleando());
    }

    @Test
    @DisplayName("acumular ate o limiar dispara, e disparar ZERA o acumulado")
    void limiarDisparaEZera() {
        StaggerState estado = new StaggerState(regras());
        assertEquals(StaggerResult.ACUMULOU, estado.acumular(1L, 8.0F));
        assertEquals(6.0F, estado.acumulado());
        assertEquals(StaggerResult.DISPAROU, estado.acumular(2L, 8.0F));
        assertEquals(0.0F, estado.acumulado(),
                "Guardar o excedente faria o proximo golpe disparar de graca, e o mob"
                        + " cambalearia em cadeia sem nunca sair do estado.");
        assertTrue(estado.cambaleando());
        assertEquals(30, estado.ticksRestantes());
    }

    @Test
    @DisplayName("o MESMO ataque nao conta duas vezes")
    void repeticaoDoMesmoAtaqueNaoSoma() {
        StaggerState estado = new StaggerState(regras());
        assertEquals(StaggerResult.ACUMULOU, estado.acumular(7L, 9.0F));
        assertEquals(StaggerResult.REPETIDO, estado.acumular(7L, 9.0F),
                "Um golpe processado por dois handlers, ou repetido por lag, acumularia"
                        + " duas vezes -- e o mob cambalearia com metade do esforco, o que e"
                        + " plausivel demais para alguem notar sem medir.");
        assertEquals(7.0F, estado.acumulado());
    }

    @Test
    @DisplayName("o acumulado decai, e sem apanhar o mob volta ao zero")
    void decaimentoEsqueceOAcumulado() {
        StaggerState estado = new StaggerState(regras());
        estado.acumular(1L, 8.0F);
        for (int tick = 0; tick < 12; tick++) estado.tick();
        assertEquals(0.0F, estado.acumulado(),
                "Sem decaimento, golpes espacados por minutos ainda somariam, e o stagger"
                        + " viraria um contador de vida inteira.");
    }

    @Test
    @DisplayName("tick devolve true no tick EXATO em que a janela termina")
    void fimDaJanelaEAvisado() {
        StaggerState estado = new StaggerState(regras());
        estado.acumular(1L, 20.0F);
        boolean terminou = false;
        for (int tick = 0; tick < 30; tick++) terminou = estado.tick();
        assertTrue(terminou, "Quem cambaleia precisa saber a hora de voltar ao estado anterior"
                + " sem vigiar contador -- vigiar contador e como a limpeza se espalha.");
        assertFalse(estado.cambaleando());
    }

    @Test
    @DisplayName("limpar apaga acumulado, janela e ultima instancia JUNTOS")
    void limpezaESimetrica() {
        StaggerState estado = new StaggerState(regras());
        estado.acumular(1L, 20.0F);
        estado.limpar();
        assertEquals(0.0F, estado.acumulado());
        assertFalse(estado.cambaleando());
        assertEquals(StaggerResult.ACUMULOU, estado.acumular(1L, 5.0F),
                "Depois da limpeza, a instancia 1 tem de poder contar de novo: nao ha rodada"
                        + " anterior para ela repetir.");
    }

    @Test
    @DisplayName("resistencia maior que o limiar reprova no construtor")
    void regrasImpossiveisReprovam() {
        assertThrows(IllegalArgumentException.class, () -> new StaggerRules(5.0F, 5.0F, 0.5F, 10),
                "Nenhum golpe acumularia, e o ponto fraco viraria decoracao -- sem erro nenhum.");
        assertThrows(IllegalArgumentException.class, () -> new StaggerRules(10.0F, 2.0F, 0.5F, 0));
        assertThrows(IllegalArgumentException.class, () -> new StaggerRules(0.0F, 0.0F, 0.0F, 10));
    }

    @Test
    @DisplayName("valor de stagger nao finito e recusado, e nao vira NaN acumulado")
    void valorNaoFinitoERecusado() {
        StaggerState estado = new StaggerState(regras());
        assertThrows(IllegalArgumentException.class, () -> estado.acumular(1L, Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> estado.acumular(1L, -1.0F));
    }
}
