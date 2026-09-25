package com.darkcontinent.nenfoundation.client.hud.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.nen.technique.Ren;
import com.darkcontinent.nenfoundation.nen.technique.Ten;
import com.darkcontinent.nenfoundation.nen.technique.Zetsu;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O portao das transicoes da HUD.
 *
 * <p>Toda a curva e provada sem esperar um tick, porque o relogio entra como
 * argumento. Se algum dia alguem trocar isso por {@code System.nanoTime}, este
 * arquivo vira um {@code Thread.sleep} e para de valer alguma coisa.
 */
class AnimacoesDaHudTest {

    private static final Optional<net.minecraft.resources.ResourceLocation> NADA =
            Optional.empty();

    @Test
    @DisplayName("a HUD nasce assentada -- entrar no mundo nao e uma transicao")
    void oPrimeiroQuadroNaoAnima() {
        AnimacoesDaHud a = new AnimacoesDaHud();
        a.observar(100.0D, 20.0F, Optional.of(Ten.ID));

        assertEquals(0.0F, a.flashDeDano(100.0D),
                "um flash na entrada faria a HUD piscar toda vez que alguem conecta");
        assertEquals(1.0F, a.fadeDoChip(100.0D), "o chip nasce aceso");
        assertEquals(0.0F, a.supressao(100.0D));
    }

    @Test
    @DisplayName("so a QUEDA de vida acende o flash; curar nao")
    void curarNaoPisca() {
        AnimacoesDaHud a = new AnimacoesDaHud();
        a.observar(0.0D, 20.0F, NADA);

        a.observar(1.0D, 14.0F, NADA);
        assertEquals(1.0F, a.flashDeDano(1.0D), "a pancada precisa acender");

        a.observar(40.0D, 20.0F, NADA);
        assertEquals(0.0F, a.flashDeDano(40.0D),
                "um flash na regeneracao faria a barra piscar o tempo todo");
    }

    @Test
    @DisplayName("o flash decai ate zero e nao volta sozinho")
    void oFlashDecai() {
        AnimacoesDaHud a = new AnimacoesDaHud();
        a.observar(0.0D, 20.0F, NADA);
        a.observar(1.0D, 10.0F, NADA);

        float cheio = a.flashDeDano(1.0D);
        float meio = a.flashDeDano(4.0D);
        float fim = a.flashDeDano(30.0D);

        assertTrue(cheio > meio && meio > fim, "a curva tem de ser monotonica");
        assertEquals(0.0F, fim, "o flash precisa terminar exatamente em zero");
    }

    @Test
    @DisplayName("o chip faz fade quando o estado muda, e nao quando ele repete")
    void oFadeSoDisparaNaBorda() {
        AnimacoesDaHud a = new AnimacoesDaHud();
        a.observar(0.0D, 20.0F, Optional.of(Ten.ID));
        a.observar(1.0D, 20.0F, Optional.of(Ten.ID));
        assertEquals(1.0F, a.fadeDoChip(1.0D), "estado repetido nao e transicao");

        a.observar(2.0D, 20.0F, Optional.of(Ren.ID));
        assertEquals(0.0F, a.fadeDoChip(2.0D), "o chip novo nasce apagado");
        assertTrue(a.fadeDoChip(3.0D) > 0.0F, "e acende");
        assertEquals(1.0F, a.fadeDoChip(20.0D), "ate assentar");
    }

    @Test
    @DisplayName("ligar e desligar uma tecnica tambem e borda de chip")
    void desligarTudoTambemFazFade() {
        AnimacoesDaHud a = new AnimacoesDaHud();
        a.observar(0.0D, 20.0F, Optional.of(Ren.ID));
        a.observar(1.0D, 20.0F, NADA);
        assertEquals(0.0F, a.fadeDoChip(1.0D));
    }

    @Test
    @DisplayName("a supressao de Zetsu sobe por rampa, e nao por estalo")
    void zetsuEntraDevagar() {
        AnimacoesDaHud a = new AnimacoesDaHud();
        a.observar(0.0D, 20.0F, Optional.of(Ten.ID));

        a.observar(1.0D, 20.0F, Optional.of(Zetsu.ID));
        assertEquals(0.0F, a.supressao(1.0D), "no primeiro quadro a cor e a de repouso");
        assertTrue(a.supressao(4.0D) > 0.0F && a.supressao(4.0D) < 1.0F,
                "no meio da rampa ela tem de estar no meio");
        assertEquals(1.0F, a.supressao(30.0D), "e terminar suprimida por inteiro");
    }

    @Test
    @DisplayName("sair de Zetsu volta pela mesma rampa")
    void zetsuSaiDevagar() {
        AnimacoesDaHud a = new AnimacoesDaHud();
        a.observar(0.0D, 20.0F, Optional.of(Zetsu.ID));
        a.observar(1.0D, 20.0F, Optional.of(Zetsu.ID));
        assertEquals(1.0F, a.supressao(30.0D));

        a.observar(31.0D, 20.0F, Optional.of(Ten.ID));
        assertEquals(1.0F, a.supressao(31.0D), "a volta comeca de onde estava");
        assertTrue(a.supressao(35.0D) < 1.0F);
        assertEquals(0.0F, a.supressao(60.0D));
    }

    @Test
    @DisplayName("piscar Zetsu no meio da rampa nao faz a cor saltar")
    void aRampaEContinuaAoInverterNoMeio() {
        AnimacoesDaHud a = new AnimacoesDaHud();
        a.observar(0.0D, 20.0F, NADA);
        a.observar(1.0D, 20.0F, Optional.of(Zetsu.ID));

        float noMeio = a.supressao(6.0D);
        assertTrue(noMeio > 0.1F && noMeio < 0.9F, "preparo do caso: " + noMeio);

        a.observar(6.0D, 20.0F, NADA);
        assertEquals(noMeio, a.supressao(6.0D), 0.001F,
                "ao inverter, a rampa tem de continuar do valor atual -- saltar para "
                        + "a ponta e o defeito que o congelamento evita");
    }

    @Test
    @DisplayName("o tempo andando para tras reinicia tudo")
    void trocarDeMundoNaoDeixaAnimacaoPendurada() {
        AnimacoesDaHud a = new AnimacoesDaHud();
        a.observar(9000.0D, 20.0F, Optional.of(Zetsu.ID));
        a.observar(9001.0D, 4.0F, Optional.of(Zetsu.ID));
        assertTrue(a.flashDeDano(9001.0D) > 0.0F);

        // Mundo novo: o gameTime recomeca.
        a.observar(3.0D, 20.0F, Optional.of(Ten.ID));
        assertEquals(0.0F, a.flashDeDano(3.0D),
                "o flash da sessao anterior ficaria em andamento por horas");
        assertEquals(1.0F, a.fadeDoChip(3.0D));
        assertEquals(0.0F, a.supressao(3.0D));
    }

    @Test
    @DisplayName("vida NaN nao contamina a comparacao do quadro seguinte")
    void vidaInvalidaNaoEnvenenaOEstado() {
        AnimacoesDaHud a = new AnimacoesDaHud();
        a.observar(0.0D, 20.0F, NADA);
        a.observar(1.0D, Float.NaN, NADA);
        a.observar(2.0D, 10.0F, NADA);

        assertTrue(a.flashDeDano(2.0D) > 0.0F,
                "a queda de 20 para 10 tem de acender mesmo com um quadro invalido "
                        + "no meio -- guardar NaN faria toda comparacao futura ser falsa");
    }

    @Test
    void limparVoltaAoEstadoInicial() {
        AnimacoesDaHud a = new AnimacoesDaHud();
        a.observar(0.0D, 20.0F, Optional.of(Zetsu.ID));
        a.observar(1.0D, 5.0F, Optional.of(Zetsu.ID));
        a.limpar();

        assertEquals(0.0F, a.supressao(1.0D));
        assertEquals(0.0F, a.flashDeDano(1.0D));
    }
}
