package com.darkcontinent.nenfoundation.enemy.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao do SALTO: ele tem telegrafo, faixa e limite fisico.
 *
 * <p>Nenhuma das falhas que este arquivo segura levanta excecao em jogo. Um
 * salto sem aviso, um salto que nunca sai e um salto que sempre pousa curto
 * produzem o mesmo verde em todos os outros portoes -- e tres relatos de bug
 * diferentes, todos impossiveis de reproduzir de proposito.</p>
 */
class RegrasDeSaltoTest {

    private static final int TELEGRAFO = 16;
    private static final double MINIMO = 2.4D;
    private static final double MAXIMO = 4.5D;
    private static final double DESNIVEL = 1.0D;
    private static final double HORIZONTAL = 0.62D;
    private static final double VERTICAL = 0.42D;

    private static RegrasDeSalto regras() {
        return new RegrasDeSalto(TELEGRAFO, MINIMO, MAXIMO, DESNIVEL, HORIZONTAL, VERTICAL);
    }

    // ------------------------------------------------------------ caso normal

    @Test
    @DisplayName("dentro da faixa, no chao e com a recarga pronta, o salto sai")
    void dentroDaFaixaOSaltoSai() {
        RegrasDeSalto salto = regras();
        assertEquals(DecisaoDeSalto.SALTAR, salto.decidir(3.5D, 0.0D, true, true));
        assertEquals(DecisaoDeSalto.SALTAR, salto.decidir(MINIMO, 0.0D, true, true),
                "a borda de baixo pertence ao salto: recusa-la encolheria a faixa declarada sem"
                        + " que nada dissesse por que");
        assertEquals(DecisaoDeSalto.SALTAR, salto.decidir(MAXIMO, DESNIVEL, true, true),
                "a borda de cima tambem, inclusive com o desnivel maximo");
        assertEquals(DecisaoDeSalto.SALTAR, salto.decidir(3.5D, -4.0D, true, true),
                "alvo ABAIXO nao e obstaculo: cair e de graca, e so a subida custa impulso");
    }

    // --------------------------------------------------------------- recusado

    @Test
    @DisplayName("cada recusa tem motivo proprio, e a ordem delas e a ordem do custo")
    void cadaRecusaTemMotivo() {
        RegrasDeSalto salto = regras();

        assertEquals(DecisaoDeSalto.EM_RECARGA, salto.decidir(3.5D, 0.0D, true, false));
        assertEquals(DecisaoDeSalto.EM_RECARGA, salto.decidir(Double.MAX_VALUE, 99.0D, false, false),
                "recarga vem antes de tudo: um bicho em recarga nao pode gastar a medida de"
                        + " geometria todo tick, por bicho, numa colonia inteira");

        assertEquals(DecisaoDeSalto.SEM_APOIO, salto.decidir(3.5D, 0.0D, false, true),
                "saltar no ar SOMA impulsos, e o bicho atravessa o cenario voando com o mesmo"
                        + " dano e o mesmo log limpo");
        assertEquals(DecisaoDeSalto.PERTO_DEMAIS, salto.decidir(MINIMO - 0.01D, 0.0D, true, true),
                "saltar em cima de quem ja esta ao alcance gasta o telegrafo inteiro para viajar"
                        + " zero blocos");
        assertEquals(DecisaoDeSalto.LONGE_DEMAIS, salto.decidir(MAXIMO + 0.01D, 0.0D, true, true));
        assertEquals(DecisaoDeSalto.SUBIDA_DEMAIS,
                salto.decidir(3.5D, DESNIVEL + 0.01D, true, true),
                "aceitar um alvo mais alto do que o impulso sobe faz o bicho bater na parede e"
                        + " cair de volta com a recarga gasta -- ele so PARECE estar tentando");
    }

    @Test
    @DisplayName("medida quebrada nao decola")
    void medidaQuebradaNaoDecola() {
        RegrasDeSalto salto = regras();
        assertThrows(IllegalArgumentException.class,
                () -> salto.decidir(Double.NaN, 0.0D, true, true));
        assertThrows(IllegalArgumentException.class,
                () -> salto.decidir(3.0D, Double.NaN, true, true));
        assertThrows(IllegalArgumentException.class, () -> salto.decidir(-1.0D, 0.0D, true, true));
    }

    // ------------------------------------------- os casos que DEVEM reprovar

    @Test
    @DisplayName("PORTAO: telegrafo abaixo do minimo legivel e recusado")
    void telegrafoCurtoEhRecusado() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeSalto(RegrasDeSalto.TELEGRAFO_MINIMO - 1, MINIMO, MAXIMO,
                        DESNIVEL, HORIZONTAL, VERTICAL));
        assertTrue(erro.getMessage().contains("teleporte"),
                "a recusa tem de dizer o que o jogador VE quando o aviso some: " + erro.getMessage());

        // O minimo exato passa. Regua que reprova o limite declarado transforma o
        // proprio numero documentado numa configuracao proibida.
        assertEquals(RegrasDeSalto.TELEGRAFO_MINIMO,
                new RegrasDeSalto(RegrasDeSalto.TELEGRAFO_MINIMO, MINIMO, MAXIMO, DESNIVEL,
                        HORIZONTAL, VERTICAL).ticksDeTelegrafo());
    }

    @Test
    @DisplayName("PORTAO: faixa vazia e recusada -- o salto nunca sairia, e nada acusaria")
    void faixaVaziaEhRecusada() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeSalto(TELEGRAFO, MAXIMO, MINIMO, DESNIVEL, HORIZONTAL, VERTICAL));
        assertTrue(erro.getMessage().contains("NUNCA sai"),
                "sem essa frase a mensagem so acusa, e nao ensina: " + erro.getMessage());
    }

    @Test
    @DisplayName("PORTAO: salto que nao sobe o desnivel aceito, e salto que sempre pousa curto")
    void balisticaImpossivelEhRecusada() {
        RegrasDeSalto salto = regras();
        assertTrue(salto.alturaEstimada() >= DESNIVEL,
                "a configuracao em uso tem de satisfazer a propria conta que o portao cobra;"
                        + " altura estimada " + salto.alturaEstimada());
        assertTrue(salto.alcanceHorizontalEstimado() >= MINIMO,
                "alcance estimado " + salto.alcanceHorizontalEstimado());

        // Vertical fraco para o desnivel aceito: ele se compromete com um salto
        // que nao alcanca, bate na parede e cai com a recarga gasta.
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeSalto(TELEGRAFO, MINIMO, MAXIMO, 3.0D, HORIZONTAL, VERTICAL));

        // Horizontal fraco para a faixa: ele sempre pousa CURTO, e o jogador
        // aprende que o salto e inofensivo.
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeSalto(TELEGRAFO, MINIMO, MAXIMO, DESNIVEL, 0.05D, VERTICAL));

        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeSalto(TELEGRAFO, MINIMO, MAXIMO, DESNIVEL, HORIZONTAL, 0.0D),
                "impulso vertical zero da um 'salto' que nao sai do chao");
    }
}
