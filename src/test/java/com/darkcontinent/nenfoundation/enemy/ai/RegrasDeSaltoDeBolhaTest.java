package com.darkcontinent.nenfoundation.enemy.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A locomocao do Bubble Horse provada sem mundo, sem entidade e sem servidor.
 *
 * <p>Os numeros aqui sao do TESTE, e nao os de producao: quem prova os valores
 * que vao para o jogo e {@code BubbleHorseTuningTest}. Misturar os dois faria um
 * ajuste de balanceamento reprovar a regra, e a proxima pessoa mexeria na regra
 * para o teste passar.</p>
 */
class RegrasDeSaltoDeBolhaTest {

    /** Pausa 10, arco 6, conforto 8 blocos, coice a 1.5. Numeros de teste. */
    private static RegrasDeSaltoDeBolha regras() {
        return new RegrasDeSaltoDeBolha(10, 6, 0.3D, 0.4D, 8.0D, 1.5D);
    }

    /** O caso normal: no chao, pausa cumprida, ameaca dentro do conforto. */
    private static DecisaoDeSaltoDeBolha comPausaCumprida(double distancia) {
        return regras().decidir(false, false, false, true, 10, distancia);
    }

    @Test
    @DisplayName("no chao, com a pausa cumprida e ameaca perto, ele salta")
    void oCasoNormalEOSalto() {
        assertEquals(DecisaoDeSaltoDeBolha.SALTA, comPausaCumprida(4.0D));
    }

    @Test
    @DisplayName("a pausa e cumprida no tick em que ela expira, e nao um depois")
    void aPausaFechaNoLimite() {
        assertEquals(DecisaoDeSaltoDeBolha.NA_PAUSA,
                regras().decidir(false, false, false, true, 9, 4.0D),
                "um tick a menos ainda e pausa");
        assertEquals(DecisaoDeSaltoDeBolha.SALTA,
                regras().decidir(false, false, false, true, 10, 4.0D),
                "adiar o salto em um tick nao da erro: desloca o ciclo inteiro em relacao ao clipe"
                        + " de animacao, e a cada volta a defasagem cresce");
    }

    @Test
    @DisplayName("cada recusa tem motivo proprio -- e nao um false unico")
    void asSeteRecusasSaoDistintas() {
        RegrasDeSaltoDeBolha r = regras();
        assertEquals(DecisaoDeSaltoDeBolha.EXAUSTO,
                r.decidir(true, false, false, true, 10, 4.0D),
                "exausto e a janela de captura: fugir aqui apagaria a mecanica do bicho");
        assertEquals(DecisaoDeSaltoDeBolha.CAMBALEANDO,
                r.decidir(false, true, false, true, 10, 4.0D));
        assertEquals(DecisaoDeSaltoDeBolha.EM_GOLPE,
                r.decidir(false, false, true, true, 10, 4.0D));
        assertEquals(DecisaoDeSaltoDeBolha.NO_AR,
                r.decidir(false, false, false, false, 10, 4.0D),
                "um segundo impulso no ar SOMA ao primeiro, e o cavalo vai parar a dez blocos"
                        + " de altura sem que nada acuse");
        assertEquals(DecisaoDeSaltoDeBolha.NA_PAUSA,
                r.decidir(false, false, false, true, 3, 4.0D));
        assertEquals(DecisaoDeSaltoDeBolha.AMEACA_LONGE,
                r.decidir(false, false, false, true, 10, 8.5D));
        assertEquals(DecisaoDeSaltoDeBolha.SEM_AMEACA,
                r.decidir(false, false, false, true, 10, Double.NaN));
    }

    @Test
    @DisplayName("a precedencia nao muda quando duas recusas valem ao mesmo tempo")
    void oQueTravaOCorpoGanhaDaFisicaEDoCompasso() {
        // Exausto E no ar E dentro da pausa. A resposta tem de ser EXAUSTO: e o
        // unico dos tres que o jogador precisa ver na tela, e publicar NO_AR aqui
        // mandaria a ferramenta de debug (e a proxima pessoa) procurar um problema
        // de fisica onde ha uma janela de captura aberta.
        assertEquals(DecisaoDeSaltoDeBolha.EXAUSTO,
                regras().decidir(true, true, true, false, 0, 4.0D));
    }

    @Test
    @DisplayName("medida degenerada NAO vira 'perto'")
    void naoFinitoNuncaAprovaOSalto() {
        RegrasDeSaltoDeBolha r = regras();
        assertEquals(DecisaoDeSaltoDeBolha.SEM_AMEACA,
                r.decidir(false, false, false, true, 10, Double.NaN));
        assertEquals(DecisaoDeSaltoDeBolha.SEM_AMEACA,
                r.decidir(false, false, false, true, 10, -1.0D),
                "distancia negativa e medida quebrada, e nao ameaca colada no bicho: aprovada,"
                        + " ela faria o cavalo disparar sozinho em campo vazio");
        assertEquals(DecisaoDeSaltoDeBolha.SEM_AMEACA,
                r.decidir(false, false, false, true, 10, Double.POSITIVE_INFINITY));
    }

    @Test
    @DisplayName("o coice so sai com a saida bloqueada, e nunca durante a janela")
    void oCoiceEDeQuemFoiEncurralado() {
        RegrasDeSaltoDeBolha r = regras();
        assertTrue(r.coiceia(false, 1.2D, true), "encurralado e com o alvo colado: ele reage");
        assertFalse(r.coiceia(false, 1.2D, false),
                "com saida livre ele FOGE. Um coice aqui transformaria a ficha do bicho num"
                        + " perseguidor de dano 3, e nenhum portao veria a troca");
        assertFalse(r.coiceia(true, 1.2D, true),
                "exausto ele nao coiceia: um golpe saindo de dentro da janela devolveria ao"
                        + " jogador o motivo para continuar batendo, que e o que perde o card");
        assertFalse(r.coiceia(false, 1.6D, true), "alem do alcance, o golpe erraria sozinho");
        assertFalse(r.coiceia(false, Double.NaN, true), "sem medida nao ha golpe");
    }

    @Test
    @DisplayName("o ciclo e a soma que o gerador de animacao copia")
    void oCicloEPausaMaisArco() {
        assertEquals(16, regras().cicloEmTicks());
    }

    // ------------------------------------------------- os casos que DEVEM reprovar

    @Test
    @DisplayName("coice com alcance maior que o conforto e RECUSADO na construcao")
    void oCoiceNaoPodeAlcancarMaisQueAFuga() {
        // ESTE E O CASO QUE DEVE REPROVAR, e ele existe porque a inversao nao da
        // erro em lugar nenhum: o bicho continuaria nascendo, atacando e passando
        // em todo portao -- so que decidindo golpear antes de decidir fugir, que e
        // o oposto da ficha dele.
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeSaltoDeBolha(10, 6, 0.3D, 0.4D, 4.0D, 4.0D));
        assertTrue(erro.getMessage().contains("decidiria golpear antes de decidir fugir"),
                "a mensagem tem de dizer o que acontece em jogo, e nao so que o numero e invalido;"
                        + " veio: " + erro.getMessage());
    }

    @Test
    @DisplayName("salto sem pausa e sem subida e recusado")
    void pausaZeroEImpulsoVerticalZeroSaoRecusados() {
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeSaltoDeBolha(0, 6, 0.3D, 0.4D, 8.0D, 1.5D),
                "pausa zero e um empurrao continuo com nome de salto");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeSaltoDeBolha(10, 6, 0.3D, 0.0D, 8.0D, 1.5D),
                "sem subida o clipe de arco mostraria um voo que o corpo nao faz");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeSaltoDeBolha(10, 0, 0.3D, 0.4D, 8.0D, 1.5D),
                "arco zero daria um ciclo que e so pausa, e a animacao copiaria isso");
    }

    @Test
    @DisplayName("contador negativo e programacao quebrada, e nao um estado do bicho")
    void ticksNegativosSaoRecusados() {
        assertThrows(IllegalArgumentException.class,
                () -> regras().decidir(false, false, false, true, -1, 4.0D));
    }
}
