package com.darkcontinent.nenfoundation.enemy.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A janela de captura do Bubble Horse provada sem mundo e sem entidade.
 *
 * <p>Os numeros aqui sao do TESTE. Os de producao -- e a coerencia deles com a
 * condicao de card publicada -- sao provados em {@code BubbleHorseTuningTest}.</p>
 */
class RegrasDeExaustaoDeBolhaTest {

    /** Colapso em 25% da vida, janela de 40 ticks, folego de 60. Numeros de teste. */
    private static RegrasDeExaustaoDeBolha regras() {
        return new RegrasDeExaustaoDeBolha(0.25D, 40, 60);
    }

    private static final float VIDA_MAXIMA = 30.0F;

    @Test
    @DisplayName("o caso normal: vida cheia, ele continua fugindo")
    void acimaDoLimiarNadaAcontece() {
        assertEquals(DecisaoDeExaustaoDeBolha.ACIMA_DO_LIMIAR,
                regras().decidir(VIDA_MAXIMA, VIDA_MAXIMA, false, false, 0, 0));
    }

    @Test
    @DisplayName("o limiar vale NO ponto, e nao um golpe depois dele")
    void exaureExatamenteNoLimiar() {
        // 25% de 30 e 7.5. A ficha e um bicho de 30 de vida: um golpe de espada de
        // ferro tira 6. Exigir "estritamente abaixo" adiaria o colapso por um golpe
        // inteiro, e esse golpe inteiro e o que mata o premio -- sem erro nenhum,
        // com o jogador concluindo que a captura nao existe.
        assertEquals(DecisaoDeExaustaoDeBolha.EXAURE,
                regras().decidir(7.5F, VIDA_MAXIMA, false, false, 0, 0));
        assertEquals(DecisaoDeExaustaoDeBolha.ACIMA_DO_LIMIAR,
                regras().decidir(7.6F, VIDA_MAXIMA, false, false, 0, 0));
    }

    @Test
    @DisplayName("a janela corre e FECHA no prazo")
    void aJanelaAbreCorreEFecha() {
        RegrasDeExaustaoDeBolha r = regras();
        assertEquals(DecisaoDeExaustaoDeBolha.JANELA_ABERTA,
                r.decidir(5.0F, VIDA_MAXIMA, false, true, 0, 0));
        assertEquals(DecisaoDeExaustaoDeBolha.JANELA_ABERTA,
                r.decidir(5.0F, VIDA_MAXIMA, false, true, 39, 0));
        assertEquals(DecisaoDeExaustaoDeBolha.JANELA_FECHOU,
                r.decidir(5.0F, VIDA_MAXIMA, false, true, 40, 0),
                "janela que nao fecha e um cavalo parado para sempre: a decisao de parar de bater"
                        + " na hora certa deixa de precisar ser tomada, e o mob inteiro some");
    }

    @Test
    @DisplayName("depois da janela, o folego impede a reabertura imediata")
    void oFolegoImpedeAJanelaPerpetua() {
        // A vida NAO sobe quando a janela expira. Sem carencia, este mesmo estado
        // -- vida abaixo do limiar, nao exausto -- devolveria EXAURE no tick
        // seguinte, para sempre. Nao da erro: da um mob permanentemente entregue,
        // com a fase de ataque certa e o log limpo.
        assertEquals(DecisaoDeExaustaoDeBolha.RECUPERANDO_O_FOLEGO,
                regras().decidir(5.0F, VIDA_MAXIMA, false, false, 0, 60));
        assertEquals(DecisaoDeExaustaoDeBolha.RECUPERANDO_O_FOLEGO,
                regras().decidir(5.0F, VIDA_MAXIMA, false, false, 0, 1));
        assertEquals(DecisaoDeExaustaoDeBolha.EXAURE,
                regras().decidir(5.0F, VIDA_MAXIMA, false, false, 0, 0),
                "com o folego esgotado a segunda janela pode abrir -- senao a primeira seria a"
                        + " unica chance da vida, e um erro de leitura custaria o card para sempre");
    }

    @Test
    @DisplayName("morrer ganha de tudo, inclusive de uma janela aberta")
    void aMorteCancelaACaptura() {
        RegrasDeExaustaoDeBolha r = regras();
        assertEquals(DecisaoDeExaustaoDeBolha.MORREU_SEM_CARD,
                r.decidir(0.0F, VIDA_MAXIMA, true, true, 5, 0),
                "um cadaver com a janela aberta ainda e um card perdido, e e esse o desfecho que"
                        + " o jogador precisa ver acontecer");
        assertEquals(DecisaoDeExaustaoDeBolha.MORREU_SEM_CARD,
                r.decidir(VIDA_MAXIMA, VIDA_MAXIMA, true, false, 0, 0),
                "morto de vida cheia -- um golpe critico de fora do combate -- tambem perde o"
                        + " card, e o aviso tem de sair igual");
    }

    // ------------------------------------------------- os casos que DEVEM reprovar

    @Test
    @DisplayName("janela de zero tick e RECUSADA na construcao")
    void janelaZeroENaoConstruivel() {
        // ESTE E O CASO QUE DEVE REPROVAR. Uma janela de zero tick nao levanta erro
        // em jogo: ela abre e fecha no mesmo tick, o cavalo nunca chega a ficar
        // parado, e o jogador conclui que a captura e impossivel -- culpando a
        // propria mira por um card que o servidor nunca oferece.
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeExaustaoDeBolha(0.25D, 0, 60));
        assertTrue(erro.getMessage().contains("nunca da para pegar"),
                "a mensagem tem de dizer o que acontece em jogo; veio: " + erro.getMessage());
    }

    @Test
    @DisplayName("folego zero e colapso em vida cheia sao recusados")
    void asOutrasDuasConfiguracoesMudasSaoRecusadas() {
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeExaustaoDeBolha(0.25D, 40, 0),
                "sem carencia a janela reabre no tick seguinte e nunca fecha de verdade");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeExaustaoDeBolha(1.0D, 40, 60),
                "colapso em vida cheia e um cavalo que nasce exausto: ele nunca foge, e a ficha"
                        + " continua parecendo certa");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeExaustaoDeBolha(0.0D, 40, 60),
                "colapso em vida zero so aconteceria depois da morte");
    }

    @Test
    @DisplayName("vida maxima invalida e contador negativo sao recusados")
    void entradaQuebradaNaoViraDecisao() {
        RegrasDeExaustaoDeBolha r = regras();
        assertThrows(IllegalArgumentException.class,
                () -> r.decidir(5.0F, 0.0F, false, false, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> r.decidir(Float.NaN, VIDA_MAXIMA, false, false, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> r.decidir(5.0F, VIDA_MAXIMA, false, true, -1, 0));
    }
}
